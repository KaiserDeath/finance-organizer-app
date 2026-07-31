package pe.moneyflow.core.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * A template that generates transactions on a schedule (rent, subscriptions, salary...).
 *
 * Two scheduling modes, chosen by whether [daysOfMonth] is empty:
 *  - **Interval mode** (`daysOfMonth` empty): fire every [interval] × [frequency] — e.g.
 *    frequency=WEEKLY, interval=2 → every two weeks; frequency=DAILY, interval=15 → every 15 days.
 *  - **Days-of-month mode** (`daysOfMonth` non-empty): fire on specific calendar days each cycle —
 *    e.g. `[15]` = the 15th; `[15, LAST_DAY_OF_MONTH]` = the 15th and the last day (semi-monthly).
 *    [interval] then means "every N months" (1 = every month, 2 = every two months) and [frequency]
 *    is ignored. A listed day that a short month lacks (e.g. 31 in February, or [LAST_DAY_OF_MONTH])
 *    clamps to that month's real last day.
 *
 * [nextRunDate] is the next date a transaction should be materialized for. When [autoCreate] is true
 * a background worker materializes due occurrences into PENDING transactions; otherwise the template
 * is informational until run manually.
 */
data class RecurringExpense(
    val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String = "PEN",
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    /** Debit/credit characteristic to stamp on generated occurrences (null = efectivo/n-a). */
    val cardKind: CardKind? = null,
    val accountId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val interval: Int = 1,
    val daysOfMonth: List<Int> = emptyList(),
    val nextRunDate: LocalDate,
    val endDate: LocalDate? = null,
    val autoCreate: Boolean = true,
    val lastGeneratedDate: LocalDate? = null,
) {
    /** True when this template fires on specific calendar days rather than a fixed interval. */
    val isMonthlyOnDays: Boolean get() = daysOfMonth.isNotEmpty()

    /** The next occurrence strictly after [from], honoring whichever scheduling mode is active. */
    fun advance(from: LocalDate): LocalDate =
        if (isMonthlyOnDays) nextDayOfMonthAfter(from) else advanceByInterval(from)

    private fun advanceByInterval(from: LocalDate): LocalDate {
        val step = interval.coerceAtLeast(1).toLong()
        return when (frequency) {
            RecurrenceFrequency.DAILY -> from.plusDays(step)
            RecurrenceFrequency.WEEKLY -> from.plusWeeks(step)
            RecurrenceFrequency.MONTHLY -> from.plusMonths(step)
            RecurrenceFrequency.YEARLY -> from.plusYears(step)
        }
    }

    /** Smallest resolved day-of-month occurrence strictly after [from], scanning month by month. */
    private fun nextDayOfMonthAfter(from: LocalDate): LocalDate {
        val monthStep = interval.coerceAtLeast(1).toLong()
        var month = YearMonth.from(from)
        repeat(MAX_MONTH_SCAN) {
            resolvedDatesIn(month).forEach { date ->
                if (date.isAfter(from)) return date
            }
            month = month.plusMonths(monthStep)
        }
        // Unreachable for sane configs; keeps the schedule advancing rather than stalling.
        return from.plusMonths(monthStep)
    }

    /** The listed days resolved to real dates in [month], clamped to the last day, sorted & deduped. */
    private fun resolvedDatesIn(month: YearMonth): List<LocalDate> =
        daysOfMonth
            .map { day ->
                if (day <= LAST_DAY_OF_MONTH || day > month.lengthOfMonth()) {
                    month.atEndOfMonth()
                } else {
                    month.atDay(day)
                }
            }
            .distinct()
            .sorted()

    companion object {
        /** Sentinel day value meaning "the last calendar day of the month" (28/29/30/31). */
        const val LAST_DAY_OF_MONTH = 0

        /** Upper bound on the month-by-month scan when finding the next day-of-month occurrence. */
        private const val MAX_MONTH_SCAN = 120

        /**
         * The first occurrence on or after [start] for the given [daysOfMonth] schedule — used to
         * seed [nextRunDate] when a template is created. For interval mode this is simply [start].
         *
         * The scan steps one month at a time (interval is intentionally ignored here) so the first
         * run always lands in or right after the start month; the configured month stride only kicks
         * in for occurrences *after* this one.
         */
        fun firstRunOnOrAfter(start: LocalDate, daysOfMonth: List<Int>): LocalDate {
            if (daysOfMonth.isEmpty()) return start
            val probe = RecurringExpense(
                id = "",
                title = "",
                amountMinor = 0,
                daysOfMonth = daysOfMonth,
                interval = 1,
                nextRunDate = start,
            )
            return probe.advance(start.minusDays(1))
        }
    }
}
