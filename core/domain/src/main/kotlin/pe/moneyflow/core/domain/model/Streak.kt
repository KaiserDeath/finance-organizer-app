package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

/**
 * One dot of the dashboard streak: whether the user logged anything that day, and whether the
 * day's spend stayed within that morning's variable daily allowance (null when no budget exists,
 * so the dot degrades to logged/not-logged).
 */
data class StreakDay(
    val date: LocalDate,
    val logged: Boolean,
    val withinAllowance: Boolean?,
)

/**
 * The last seven days, oldest first, each measured against the *variable* daily allowance as it
 * stood that morning: `(budget − paid spend before that day) / days remaining including it` —
 * the same arithmetic as [SpendingPace.remainingDailyAllowanceMinor], replayed per day.
 *
 * The streak is deliberately the only non-actionable element on the dashboard: it doesn't inform
 * a decision, it sustains the logging habit.
 */
fun streakOf(
    transactions: List<Transaction>,
    monthBudgetMinor: Long?,
    today: LocalDate,
): List<StreakDay> {
    val paidExpenses = transactions.filter {
        it.type == TransactionType.EXPENSE && it.status == TransactionStatus.PAID
    }
    val anyLoggedByDay = transactions
        .mapNotNull { it.effectiveDate }
        .toSet()
    val spentByDay = paidExpenses
        .filter { it.effectiveDate != null }
        .groupBy { it.effectiveDate!! }
        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

    return (6 downTo 0).map { back ->
        val day = today.minusDays(back.toLong())
        val logged = day in anyLoggedByDay
        val spent = spentByDay[day] ?: 0L
        val allowance = allowanceOn(day, monthBudgetMinor, spentByDay)
        StreakDay(
            date = day,
            logged = logged,
            withinAllowance = allowance?.let { spent <= it },
        )
    }
}

/** The per-day allowance as of [day]'s morning, within [day]'s own month window. */
private fun allowanceOn(
    day: LocalDate,
    monthBudgetMinor: Long?,
    spentByDay: Map<LocalDate, Long>,
): Long? {
    val budget = monthBudgetMinor?.takeIf { it > 0 } ?: return null
    val month = YearMonth.from(day)
    val spentBefore = spentByDay.entries
        .filter { (date, _) -> YearMonth.from(date) == month && date.isBefore(day) }
        .sumOf { it.value }
    val daysRemainingIncluding = month.lengthOfMonth() - day.dayOfMonth + 1
    return ((budget - spentBefore) / daysRemainingIncluding).coerceAtLeast(0)
}
