package pe.moneyflow.core.model

import java.time.LocalDate

/**
 * A template that generates transactions on a schedule (rent, subscriptions, salary...).
 * [nextRunDate] is the next date a transaction should be materialized for; [interval] is the
 * multiplier applied to [frequency] (e.g. frequency=WEEKLY, interval=2 → every two weeks).
 * When [autoCreate] is true a background worker materializes due occurrences into PENDING
 * transactions; otherwise the template is informational until run manually.
 */
data class RecurringExpense(
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String = "PEN",
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    val accountId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val interval: Int = 1,
    val nextRunDate: LocalDate,
    val endDate: LocalDate? = null,
    val autoCreate: Boolean = true,
    val lastGeneratedDate: LocalDate? = null,
) {
    /** Advances a date by one recurrence step. */
    fun advance(from: LocalDate): LocalDate {
        val step = interval.coerceAtLeast(1).toLong()
        return when (frequency) {
            RecurrenceFrequency.DAILY -> from.plusDays(step)
            RecurrenceFrequency.WEEKLY -> from.plusWeeks(step)
            RecurrenceFrequency.MONTHLY -> from.plusMonths(step)
            RecurrenceFrequency.YEARLY -> from.plusYears(step)
        }
    }
}
