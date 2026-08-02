package pe.moneyflow.feature.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.ui.util.toMonthTitle
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
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
    /** Payment methods keyed by id, so a row can resolve whether its method has a launchable app. */
    val methodsById: Map<String, PaymentMethod> = emptyMap(),
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()

    /** What next month already commits you to — the number this screen exists to answer. */
    val nextMonthTotalMinor: Long
        get() = sections.firstOrNull { it.bucket == UpcomingBucket.NEXT_MONTH }?.totalMinor ?: 0L

    /** The method behind a payment, or null when unset/unknown. */
    fun methodFor(payment: UpcomingPayment): PaymentMethod? =
        payment.transaction.paymentMethodId?.let { methodsById[it] }

    /** The method the pay sheet pre-selects: the payment's own, else the user's default. */
    fun suggestedMethodFor(payment: UpcomingPayment): PaymentMethod? =
        methodFor(payment) ?: methodsById.values.firstOrNull { it.isDefault }
}

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    getUpcoming: GetUpcomingPaymentsUseCase,
    paymentMethodRepository: PaymentMethodRepository,
    private val markTransactionPaid: MarkTransactionPaidUseCase,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val clock: Clock,
) : ViewModel() {

    // The last swipe-deleted payment, held so "Deshacer" can re-insert it verbatim.
    private var recentlyDeleted: Transaction? = null

    val uiState: StateFlow<UpcomingUiState> =
        combine(getUpcoming(), paymentMethodRepository.observeAll()) { payments, methods ->
            UpcomingUiState(
                isLoading = false,
                sections = sectionsOf(payments),
                currencyCode = payments.firstOrNull()?.transaction?.currencyCode ?: "PEN",
                methodsById = methods.associateBy { it.id },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UpcomingUiState(),
        )

    // How to revert the last settle: restore the original row (real payments) or delete the row
    // that materialized (projections). Holding the undo as data keeps both cases symmetric.
    private var lastSettleUndo: (suspend () -> Unit)? = null

    /**
     * Settles a payment as paid, optionally recording the method it was actually paid with —
     * that memory is what makes the suggested method improve with use, so it is not optional
     * on any path that knows the method.
     *
     * A real pending row is marked paid in place; a projected occurrence has no row yet, so it
     * materializes the synthetic transaction as already paid — the one and only way a forecast
     * turns into ledger data.
     */
    fun settle(payment: UpcomingPayment, methodId: String? = null) {
        val tx = payment.transaction
        if (payment.isProjected) {
            val today = LocalDate.now(clock)
            val now = Instant.now(clock)
            val newId = UUID.randomUUID().toString()
            lastSettleUndo = { deleteTransaction(newId) }
            viewModelScope.launch {
                saveTransaction(
                    tx.copy(
                        id = newId,
                        status = TransactionStatus.PAID,
                        actualDate = today,
                        paymentMethodId = methodId ?: tx.paymentMethodId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        } else {
            // `tx` is the row as it was before settling, so restoring it undoes both the status
            // flip and any method change in one step.
            lastSettleUndo = { saveTransaction(tx) }
            viewModelScope.launch { markTransactionPaid(tx.id, methodId) }
        }
    }

    /** Reverts the last [settle] (the snackbar's "Deshacer"). */
    fun undoSettle() {
        val undo = lastSettleUndo ?: return
        lastSettleUndo = null
        viewModelScope.launch { undo() }
    }

    /** Removes a payment, stashing it so [undoDelete] can restore it. */
    fun delete(transaction: Transaction) {
        recentlyDeleted = transaction
        viewModelScope.launch { deleteTransaction(transaction.id) }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { saveTransaction(toRestore) }
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

    // Next month names itself ("Agosto 2026") so the section needs no explanation.
    private fun labelFor(bucket: UpcomingBucket): String = when (bucket) {
        UpcomingBucket.DUE_NOW -> "Por pagar"
        UpcomingBucket.THIS_MONTH -> "Resto del mes"
        UpcomingBucket.NEXT_MONTH -> LocalDate.now(clock).plusMonths(1).toMonthTitle()
    }
}
