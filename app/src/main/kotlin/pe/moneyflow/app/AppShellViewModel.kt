package pe.moneyflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import javax.inject.Inject

/**
 * State the app shell itself needs, independent of any one screen.
 *
 * Currently just the overdue count, which badges the "Próximos" tab. A payment going overdue is the
 * one thing in this app that genuinely warrants pulling the user's attention from wherever they are —
 * previously it was only discoverable by navigating to the tab that would have told you.
 */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    getUpcomingPayments: GetUpcomingPaymentsUseCase,
) : ViewModel() {

    val overdueCount: StateFlow<Int> = getUpcomingPayments()
        .map { payments -> payments.count { it.isOverdue } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
