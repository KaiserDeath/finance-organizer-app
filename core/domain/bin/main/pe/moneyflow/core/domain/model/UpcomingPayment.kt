package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction

/** Time bucket a pending payment falls into, ordered by urgency. */
enum class UpcomingBucket { OVERDUE, TODAY, TOMORROW, THIS_WEEK, THIS_MONTH, LATER }

/** A pending payment plus the bucket it belongs to and its resolved category. */
data class UpcomingPayment(
    val transaction: Transaction,
    val bucket: UpcomingBucket,
    val category: Category?,
)
