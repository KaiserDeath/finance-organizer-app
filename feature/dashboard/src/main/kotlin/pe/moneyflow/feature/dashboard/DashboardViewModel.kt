package pe.moneyflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.domain.model.SpendingPace
import pe.moneyflow.core.domain.model.StreakDay
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.model.streakOf
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase
import pe.moneyflow.core.domain.usecase.GetFrequentShortcutsUseCase
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
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
    /** One-tap presets for the "De un toque" row; empty hides the row. */
    val shortcuts: List<QuickShortcut> = emptyList(),
    /** Last seven days against the variable daily allowance; empty on past months. */
    val streak: List<StreakDay> = emptyList(),
) {
    val isCurrentMonth: Boolean get() = data.month == YearMonth.now()

    /**
     * Nothing has ever been logged, and we are looking at the month where that can be fixed.
     *
     * Inicio otherwise greets a new user with four zeros and two separate explanations of the same
     * emptiness — the band's projection and budget bar collapse, the shortcuts card explains a
     * 30-day rule, and the movements card says there are no movements — while the one thing to do
     * (the FAB) is the smallest element on screen. This collapses all of that into one ask.
     *
     * `monthSpentMinor` is checked alongside `recent` because the recent list is capped: a month
     * with spend but nothing in the window is not a first run.
     */
    val isFirstRun: Boolean
        get() = !isLoading &&
            isCurrentMonth &&
            data.recent.isEmpty() &&
            data.monthSpentMinor == 0L

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
    getFrequentShortcuts: GetFrequentShortcutsUseCase,
    observeTransactions: ObserveTransactionsUseCase,
    private val settingsRepository: SettingsRepository,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now(clock))

    /** The last shortcut-logged movement, so its snackbar's deshacer can remove it. */
    private var lastShortcutId: String? = null

    fun showPreviousMonth() = selectedMonth.update { it.minusMonths(1) }

    fun showNextMonth() = selectedMonth.update { month ->
        // Never advance past the current month.
        if (month.isBefore(YearMonth.now(clock))) month.plusMonths(1) else month
    }

    fun showCurrentMonth() = selectedMonth.update { YearMonth.now(clock) }

    /**
     * Flips discreet mode and persists it, so a masked screen stays masked across launches.
     *
     * Reads the current value from the store rather than from [uiState] so the write is against
     * what is actually saved, not against a frame that may have been composed before the last one
     * landed. Presentation only — nothing here touches what is stored or computed.
     */
    fun toggleAmountsHidden() {
        viewModelScope.launch {
            val current = settingsRepository.preferences.first().amountsHidden
            settingsRepository.setAmountsHidden(!current)
        }
    }

    val uiState: StateFlow<DashboardUiState> = selectedMonth
        .flatMapLatest { month ->
            combine(
                combine(getDashboard(month), getUpcoming(), getInsights(), getBudgetsProgress()) {
                        data, upcoming, insights, budgets ->
                    CoreInputs(data, upcoming, insights, budgets)
                },
                settingsRepository.preferences,
                observeTransactions(),
                getFrequentShortcuts(),
            ) { core, prefs, transactions, inferredShortcuts ->
                val (data, upcoming, insights, budgets) = core
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
                // Real budget entities win; the onboarding target is the fallback denominator so
                // skipping budget setup degrades the hero instead of blanking it.
                val monthBudgetMinor = monthlyBudgets
                    .sumOf { it.budget.amountMinor }
                    .takeIf { monthlyBudgets.isNotEmpty() }
                    ?: prefs.monthlyBudgetMinor
                val pace = if (isCurrentMonth) {
                    SpendingPace.of(
                        month = month,
                        today = today,
                        spentMinor = data.monthSpentMinor,
                        // Only overall/monthly limits form a month denominator. Summing weekly and
                        // annual budgets into one figure would produce a number that means nothing.
                        monthBudgetMinor = monthBudgetMinor,
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
                    // Onboarding's picks win; with none, the 30-day frequency heuristic fills in.
                    shortcuts = if (isCurrentMonth) {
                        prefs.shortcuts.ifEmpty { inferredShortcuts }
                    } else {
                        emptyList()
                    },
                    streak = if (isCurrentMonth) {
                        streakOf(transactions, monthBudgetMinor, today)
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

    /** Saves the shortcut as a paid movement of today, in one tap. Undone by [undoShortcut]. */
    fun logShortcut(shortcut: QuickShortcut, currencyCode: String) {
        val id = UUID.randomUUID().toString()
        lastShortcutId = id
        viewModelScope.launch {
            saveTransaction(
                Transaction(
                    id = id,
                    title = shortcut.label,
                    amountMinor = shortcut.amountMinor,
                    currencyCode = currencyCode,
                    categoryId = shortcut.categoryId,
                    paymentMethodId = shortcut.paymentMethodId,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.PAID,
                    actualDate = LocalDate.now(clock),
                ),
            )
        }
    }

    fun undoShortcut() {
        val id = lastShortcutId ?: return
        lastShortcutId = null
        viewModelScope.launch { deleteTransaction(id) }
    }
}

/** Bundles the first four sources so the outer combine stays within the typed overloads. */
private data class CoreInputs(
    val data: DashboardData,
    val upcoming: List<UpcomingPayment>,
    val insights: List<Insight>,
    val budgets: List<BudgetProgress>,
)
