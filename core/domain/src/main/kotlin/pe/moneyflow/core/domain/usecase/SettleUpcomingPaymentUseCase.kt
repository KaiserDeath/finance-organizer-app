package pe.moneyflow.core.domain.usecase

import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.TransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * The two suspend actions behind settling one payment: do it, and put it back. Held as data rather
 * than executed inline so one settle and a batch of them can share the same two behaviours — a real
 * pending row is marked paid in place, while a projected occurrence has no row yet and materializes
 * as already paid.
 */
data class PaymentSettlement(val apply: suspend () -> Unit, val undo: suspend () -> Unit)

/**
 * Builds the settle/undo pair for an [UpcomingPayment].
 *
 * Every screen that can pay a bill (Inicio's dashboard nudge, Próximos, and the "Pagar con X"
 * action while creating a pending expense) shares this instead of each reimplementing what
 * "settled" means — the previous duplication is exactly how Inicio and AddEdit ended up disagreeing
 * about whether returning from the bank app marks a charge paid.
 */
class SettleUpcomingPaymentUseCase @Inject constructor(
    private val markTransactionPaid: MarkTransactionPaidUseCase,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val clock: Clock,
) {
    operator fun invoke(payment: UpcomingPayment, methodId: String? = null): PaymentSettlement {
        val tx = payment.transaction
        return if (payment.isProjected) {
            val today = LocalDate.now(clock)
            val now = Instant.now(clock)
            val newId = UUID.randomUUID().toString()
            val apply: suspend () -> Unit = {
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
            PaymentSettlement(apply = apply, undo = { deleteTransaction(newId) })
        } else {
            // `tx` is the row as it was before settling, so restoring it undoes both the status
            // flip and any method change in one step.
            val apply: suspend () -> Unit = { markTransactionPaid(tx.id, methodId) }
            PaymentSettlement(apply = apply, undo = { saveTransaction(tx) })
        }
    }
}
