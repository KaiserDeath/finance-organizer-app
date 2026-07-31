package pe.moneyflow.core.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Forward-looking view of a month's spending.
 *
 * The dashboard's headline number used to be a bare figure: "S/ 1,240" answers nothing on its own,
 * because a user opening a spending app wants to know whether they're *fine*, and that question needs
 * a reference. This model supplies the three references that matter — a **denominator** (the budget), a
 * **projection** (where the current rate lands by month end), and a **rate** (what's still spendable
 * per day) — so the figure can be read rather than merely displayed.
 *
 * Pure data: constructed by [of] from values the caller already has, with no repository access, so the
 * arithmetic is directly testable.
 *
 * Projection is only meaningful for a month still in progress. For a finished month [isComplete] is
 * true, [projectedMonthEndMinor] equals [spentMinor], and the per-day figures are null — there is no
 * "at this rate" for a month that has already happened.
 */
data class SpendingPace(
    val month: YearMonth,
    val spentMinor: Long,
    /** Days already elapsed in [month], clamped to 1..daysInMonth. */
    val elapsedDays: Int,
    val daysInMonth: Int,
    /** Sum of the user's monthly budgets, or null when none are configured. */
    val monthBudgetMinor: Long?,
    /**
     * Expenses still committed for the rest of [month] — pending charges and projected recurring
     * occurrences. Answers "how much of my month is already spoken for?", which no screen said before.
     */
    val committedRemainingMinor: Long,
) {
    /** True once the month is over, so no projection applies. */
    val isComplete: Boolean get() = elapsedDays >= daysInMonth

    val daysRemaining: Int get() = (daysInMonth - elapsedDays).coerceAtLeast(0)

    /** Average spend per elapsed day. */
    val dailyAverageMinor: Long get() = spentMinor / elapsedDays.coerceAtLeast(1)

    /**
     * Straight-line month-end estimate at the current rate, plus what's already committed.
     *
     * Committed amounts are added rather than assumed to be inside the run rate: a rent charge due on
     * the 28th is not reflected in the first week's daily average, and a projection that ignores it
     * would read reassuringly low for most of the month.
     */
    val projectedMonthEndMinor: Long
        get() = if (isComplete) {
            spentMinor
        } else {
            spentMinor + (dailyAverageMinor * daysRemaining) + committedRemainingMinor
        }

    /** Share of the budget already spent (uncapped, so overspend is visible), null without a budget. */
    val budgetFraction: Float?
        get() = monthBudgetMinor?.takeIf { it > 0 }?.let { spentMinor.toFloat() / it }

    val remainingBudgetMinor: Long?
        get() = monthBudgetMinor?.let { it - spentMinor }

    /**
     * What can be spent per remaining day and still land on budget. Null without a budget or once the
     * month is over; zero (not negative) when the budget is already exhausted, because "you may spend
     * −S/ 12 per day" is not a sentence.
     */
    val remainingDailyAllowanceMinor: Long?
        get() {
            val remaining = remainingBudgetMinor ?: return null
            if (isComplete || daysRemaining == 0) return null
            return (remaining / daysRemaining).coerceAtLeast(0)
        }

    val isOverBudget: Boolean
        get() = monthBudgetMinor?.let { spentMinor > it } == true

    /** On track to exceed the budget by month end, even though it hasn't happened yet. */
    val isProjectedOverBudget: Boolean
        get() = !isOverBudget && monthBudgetMinor?.let { projectedMonthEndMinor > it } == true

    companion object {
        fun of(
            month: YearMonth,
            today: LocalDate,
            spentMinor: Long,
            monthBudgetMinor: Long?,
            committedRemainingMinor: Long,
        ): SpendingPace {
            val daysInMonth = month.lengthOfMonth()
            // A month in the past is fully elapsed; one in the future hasn't started, so treat day 1
            // as elapsed to keep the daily average from dividing by zero.
            val elapsed = when {
                YearMonth.from(today) == month -> today.dayOfMonth
                month.isBefore(YearMonth.from(today)) -> daysInMonth
                else -> 1
            }
            return SpendingPace(
                month = month,
                spentMinor = spentMinor,
                elapsedDays = elapsed.coerceIn(1, daysInMonth),
                daysInMonth = daysInMonth,
                monthBudgetMinor = monthBudgetMinor,
                committedRemainingMinor = committedRemainingMinor,
            )
        }
    }
}
