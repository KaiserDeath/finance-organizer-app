package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Transaction
import java.time.LocalDate

/**
 * Time bucket a pending payment falls into, ordered by urgency.
 *
 * Three buckets on purpose: what needs acting on now, what is left of this month, and what next
 * month commits you to. [DUE_NOW] also collects pending payments with no date at all — they need
 * attention precisely because they are unresolved.
 */
enum class UpcomingBucket { DUE_NOW, THIS_MONTH, NEXT_MONTH }

/**
 * A payment the user still owes, plus the bucket it belongs to and its resolved category.
 *
 * Two flavors share this type:
 *  - **materialized** ([isProjected] false): a real PENDING [Transaction] row, fully actionable.
 *  - **projected** ([isProjected] true): a future occurrence derived from a recurring template and
 *    never persisted. Its [transaction] is synthetic (blank id) and carries `recurringId` so the UI
 *    can reach the template. Nothing may delete or edit it; paying it materializes it first.
 */
data class UpcomingPayment(
    val transaction: Transaction,
    val bucket: UpcomingBucket,
    val category: Category?,
    /** When the payment is expected. Null only for a dateless materialized row. */
    val dueDate: LocalDate? = null,
    /** True when this occurrence exists only as a forecast, with no row behind it. */
    val isProjected: Boolean = false,
    /** True when [dueDate] is already past. Always false for projections. */
    val isOverdue: Boolean = false,
)

/**
 * The method a pay sheet should preselect: the payment's own method, else the user's default.
 *
 * Shared by every screen offering a pay sheet (Inicio, Próximos), so the preselection means the
 * same thing regardless of which screen opened it.
 */
fun UpcomingPayment.suggestedMethod(methodsById: Map<String, PaymentMethod>): PaymentMethod? =
    transaction.paymentMethodId?.let { methodsById[it] }
        ?: methodsById.values.firstOrNull { it.isDefault }
