package pe.moneyflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase
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
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboard: GetDashboardUseCase,
    getUpcoming: GetUpcomingPaymentsUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        combine(getDashboard(), getUpcoming()) { data, upcoming ->
            val overdue = upcoming.count { it.bucket == UpcomingBucket.OVERDUE }
            val dueSoon = upcoming.count {
                it.bucket == UpcomingBucket.TODAY ||
                    it.bucket == UpcomingBucket.TOMORROW ||
                    it.bucket == UpcomingBucket.THIS_WEEK
            }
            val nudge = if (overdue + dueSoon > 0) {
                UpcomingNudge(
                    overdueCount = overdue,
                    dueSoonCount = dueSoon,
                    totalAmountMinor = upcoming
                        .filter {
                            it.bucket == UpcomingBucket.OVERDUE ||
                                it.bucket == UpcomingBucket.TODAY ||
                                it.bucket == UpcomingBucket.TOMORROW ||
                                it.bucket == UpcomingBucket.THIS_WEEK
                        }
                        .sumOf { it.transaction.amountMinor },
                )
            } else {
                null
            }
            DashboardUiState(isLoading = false, data = data, upcomingNudge = nudge)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true),
        )
}
