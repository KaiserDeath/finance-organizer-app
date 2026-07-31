package pe.moneyflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.domain.model.SpendingPace
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.model.BudgetPeriod
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** A dashboard prompt about payments that need attention, or null when nothing is due. */
data class UpcomingNudge(
    val overdueCount: Int,
    val dueSoonCount: Int,
    val totalAmountMinor: Long,
) {
    val actionableCount: Int get() = overdueCount + dueSoonCount
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: DashboardData = DashboardData.empty(YearMonth.now()),
    val upcomingNudge: UpcomingNudge? = null,
    val topInsight: Insight? = null,
    /**
     * Pace for the viewed month, or null when it isn't computable.
     *
     * Only present for the **current** month. Budgets are evaluated against today's period, so
     * showing budget progress or an "at this rate" projection while looking at a past month would be
     * comparing that month's spend against this month's limits.
     */
    val pace: SpendingPace? = null,
    /** Top budgets by how close they are to their limit — the ones worth surfacing first. */
    val topBudgets: List<BudgetProgress> = emptyList(),
) {
    val isCurrentMonth: Boolean get() = data.month == YearMonth.now()

    /** Whether an earlier month can be reached; the app has no data before its first transaction. */
    val canGoBack: Boolean get() = true

    /** No forward navigation past the current month — there is nothing to show yet. */
    val canGoForward: Boolean get() = data.month.isBefore(YearMonth.now())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboard: GetDashboardUseCase,
    getUpcoming: GetUpcomingPaymentsUseCase,
    getInsights: GetInsightsUseCase,
    getBudgetsProgress: GetBudgetsProgressUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now(clock))

    fun showPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun showNextMonth() = selectedMonth.update { month ->
        // Never advance past the current month.
        if (month.isBefore(YearMonth.now(clock))) month.plusMonths(1) else month
    }

    fun showCurrentMonth() = selectedMonth.update { YearMonth.now(clock) }

    val uiState: StateFlow<DashboardUiState> = selectedMonth
        .flatMapLatest { month ->
            combine(
                getDashboard(month),
                getUpcoming(),
                getInsights(),
                getBudgetsProgress(),
            ) { data, upcoming, insights, budgets ->
                val today = LocalDate.now(clock)
                val isCurrentMonth = YearMonth.from(today) == month

                // The nudge is about things the user can act on right now, so projected occurrences
                // are out: they have no row behind them yet. "Soon" stays a one-week horizon.
                val soonCutoff = today.plusDays(7)
                val actionable = upcoming.filter { payment ->
                    !payment.isProjected && payment.dueDate?.isAfter(soonCutoff) != true
                }
                val overdue = actionable.count { it.isOverdue }
                val dueSoon = actionable.size - overdue
                val nudge = if (actionable.isNotEmpty()) {
                    UpcomingNudge(
                        overdueCount = overdue,
                        dueSoonCount = dueSoon,
                        totalAmountMinor = actionable.sumOf { it.transaction.amountMinor },
                    )
                } else {
                    null
                }

                // Surface the most important insight, skipping bill kinds already covered by the nudge.
                val severityRank = mapOf(
                    InsightSeverity.WARNING to 0,
                    InsightSeverity.INFO to 1,
                    InsightSeverity.POSITIVE to 2,
                )
                val topInsight = insights
                    .filter {
                        it.kind != InsightKind.UPCOMING_BILLS && it.kind != InsightKind.OVERDUE_BILLS
                    }
                    .minByOrNull { severityRank[it.severity] ?: 3 }

                val monthlyBudgets = budgets.filter { it.budget.period == BudgetPeriod.MONTHLY }
                val pace = if (isCurrentMonth) {
                    SpendingPace.of(
                        month = month,
                        today = today,
                        spentMinor = data.monthSpentMinor,
                        // Only overall/monthly limits form a month denominator. Summing weekly and
                        // annual budgets into one figure would produce a number that means nothing.
                        monthBudgetMinor = monthlyBudgets
                            .sumOf { it.budget.amountMinor }
                            .takeIf { monthlyBudgets.isNotEmpty() },
                        // What's still owed before month end: pending rows plus projected recurring
                        // occurrences. Already-paid charges are in monthSpentMinor, not here.
                        committedRemainingMinor = upcoming
                            .filter { payment ->
                                val due = payment.dueDate
                                due != null && !due.isBefore(today) &&
                                    YearMonth.from(due) == month
                            }
                            .sumOf { it.transaction.amountMinor },
                    )
                } else {
                    null
                }

                DashboardUiState(
                    isLoading = false,
                    data = data,
                    upcomingNudge = nudge,
                    topInsight = topInsight,
                    pace = pace,
                    // Closest to the limit first — a budget at 95% matters more than a bigger one at 10%.
                    topBudgets = if (isCurrentMonth) {
                        monthlyBudgets.sortedByDescending { it.fraction }.take(3)
                    } else {
                        emptyList()
                    },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true),
        )
}
