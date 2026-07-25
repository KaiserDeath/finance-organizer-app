package pe.moneyflow.feature.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import javax.inject.Inject

data class UpcomingSection(
    val bucket: UpcomingBucket,
    val label: String,
    val items: List<UpcomingPayment>,
    val totalMinor: Long,
)

data class UpcomingUiState(
    val isLoading: Boolean = true,
    val sections: List<UpcomingSection> = emptyList(),
    val currencyCode: String = "PEN",
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
}

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    getUpcoming: GetUpcomingPaymentsUseCase,
    private val markTransactionPaid: MarkTransactionPaidUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
) : ViewModel() {

    val uiState: StateFlow<UpcomingUiState> = getUpcoming()
        .map { payments ->
            UpcomingUiState(
                isLoading = false,
                sections = sectionsOf(payments),
                currencyCode = payments.firstOrNull()?.transaction?.currencyCode ?: "PEN",
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UpcomingUiState(),
        )

    fun markPaid(id: String) {
        viewModelScope.launch { markTransactionPaid(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteTransaction(id) }
    }

    private fun sectionsOf(payments: List<UpcomingPayment>): List<UpcomingSection> =
        UpcomingBucket.entries.mapNotNull { bucket ->
            val items = payments.filter { it.bucket == bucket }
            if (items.isEmpty()) {
                null
            } else {
                UpcomingSection(
                    bucket = bucket,
                    label = labelFor(bucket),
                    items = items,
                    totalMinor = items.sumOf { it.transaction.amountMinor },
                )
            }
        }

    private fun labelFor(bucket: UpcomingBucket): String = when (bucket) {
        UpcomingBucket.OVERDUE -> "Vencidos"
        UpcomingBucket.TODAY -> "Hoy"
        UpcomingBucket.TOMORROW -> "Mañana"
        UpcomingBucket.THIS_WEEK -> "Esta semana"
        UpcomingBucket.THIS_MONTH -> "Este mes"
        UpcomingBucket.LATER -> "Más adelante"
    }
}
