package pe.moneyflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
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
    val data: DashboardData = DashboardData.Empty,
    val upcomingNudge: UpcomingNudge? = null,
    val topInsight: Insight? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboard: GetDashboardUseCase,
    getUpcoming: GetUpcomingPaymentsUseCase,
    getInsights: GetInsightsUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        combine(getDashboard(), getUpcoming(), getInsights()) { data, upcoming, insights ->
            val soonBuckets = setOf(
                UpcomingBucket.OVERDUE,
                UpcomingBucket.TODAY,
                UpcomingBucket.TOMORROW,
                UpcomingBucket.THIS_WEEK,
            )
            val overdue = upcoming.count { it.bucket == UpcomingBucket.OVERDUE }
            val dueSoon = upcoming.count { it.bucket in soonBuckets && it.bucket != UpcomingBucket.OVERDUE }
            val nudge = if (overdue + dueSoon > 0) {
                UpcomingNudge(
                    overdueCount = overdue,
                    dueSoonCount = dueSoon,
                    totalAmountMinor = upcoming
                        .filter { it.bucket in soonBuckets }
                        .sumOf { it.transaction.amountMinor },
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
                .filter { it.kind != InsightKind.UPCOMING_BILLS && it.kind != InsightKind.OVERDUE_BILLS }
                .minByOrNull { severityRank[it.severity] ?: 3 }

            DashboardUiState(
                isLoading = false,
                data = data,
                upcomingNudge = nudge,
                topInsight = topInsight,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true),
        )
}
