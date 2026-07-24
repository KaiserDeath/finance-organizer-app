package pe.moneyflow.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * A single money movement. Amounts are stored as [amountMinor] — minor currency units
 * (céntimos for PEN) as a [Long] — never as floating point, to keep arithmetic exact.
 */
data class Transaction(
    val id: String,
    val title: String,
    val description: String? = null,
    val amountMinor: Long,
    val currencyCode: String = "PEN",
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    val accountId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val status: TransactionStatus = TransactionStatus.PAID,
    val priority: Priority = Priority.NORMAL,
    /** When the payment is expected (for pending/upcoming payments). */
    val estimatedDate: LocalDate? = null,
    /** When the payment actually happened. Drives "spent" totals. */
    val actualDate: LocalDate? = null,
    val recurringId: String? = null,
    val installmentPlanId: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    /** The date this transaction "counts" on: actual if known, otherwise the estimate. */
    val effectiveDate: LocalDate?
        get() = actualDate ?: estimatedDate
}
