package pe.moneyflow.core.domain.model

import java.time.YearMonth

/**
 * Day-by-day running total of expenses for a month, for the cash-flow curve.
 *
 * This is the one chart shape the app was missing, and it answers a question bars cannot. Bars tell you
 * "how much per bucket"; only a cumulative curve tells you **which direction you're heading** — whether
 * you're ahead of last month at the same point, and whether the gap is widening or closing. That is the
 * comparison that actually changes behaviour mid-month, because it's still actionable when you see it.
 *
 * [current] is truncated at today: drawing a flat line across the rest of the month would read as
 * "spending stopped", which is the opposite of the truth. [previous] always spans its full month, so
 * the two lines can be compared at any x.
 */
data class CumulativeSpend(
    val month: YearMonth,
    /** Running total per day of [month], index 0 = day 1. Ends at today for an in-progress month. */
    val current: List<Long>,
    /** Running total per day of the preceding month, over its whole length. */
    val previous: List<Long>,
    val currencyCode: String,
) {
    val currentTotalMinor: Long get() = current.lastOrNull() ?: 0L

    /**
     * The previous month's running total at the same day index the current series has reached, so
     * "vs last month" compares like with like instead of a partial month against a complete one.
     */
    val previousAtSameDayMinor: Long
        get() = if (current.isEmpty()) 0L else previous.getOrElse(current.lastIndex) {
            previous.lastOrNull() ?: 0L
        }

    /** Signed difference at the same point in the month; positive means spending more than last month. */
    val deltaAtSameDayMinor: Long get() = currentTotalMinor - previousAtSameDayMinor

    /** Highest value across both series — the y-axis ceiling both lines must share to be comparable. */
    val peakMinor: Long
        get() = maxOf(current.maxOrNull() ?: 0L, previous.maxOrNull() ?: 0L)

    val hasData: Boolean get() = peakMinor > 0

    companion object {
        fun empty(month: YearMonth, currencyCode: String = "PEN") =
            CumulativeSpend(month, emptyList(), emptyList(), currencyCode)

        /**
         * Builds a running total across [dayCount] days from per-day amounts keyed by day-of-month.
         * Days with no spend carry the previous total forward rather than dropping to zero.
         */
        fun runningTotal(dayCount: Int, amountsByDay: Map<Int, Long>): List<Long> {
            var acc = 0L
            return (1..dayCount).map { day ->
                acc += amountsByDay[day] ?: 0L
                acc
            }
        }
    }
}
