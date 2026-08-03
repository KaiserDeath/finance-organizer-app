package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Everything the Analytics screen shows, computed by
 * [pe.moneyflow.core.domain.usecase.GetAnalyticsUseCase]. Amounts are minor units (céntimos).
 *
 * **Two windows live here, and every field says which one it belongs to.** The `month*` fields
 * describe the current calendar month and nothing else; [months] and [weekdays] span the rolling
 * multi-month window. They used to share one set of names, so the screen printed six-month sums
 * under labels a reader takes for "this month" — and the month total disagreed with the hero on
 * Inicio, which was always month-scoped. A field whose window is not in its name is how that
 * happened; keep the prefix.
 */
data class AnalyticsData(
    /** One entry per calendar month in the window, oldest first. */
    val months: List<MonthlyPoint>,
    /** Expense totals per category **for the current month**, largest first. */
    val monthCategoryBreakdown: List<CategorySpend>,
    /** Expense totals per weekday (Mon..Sun) across the whole window. */
    val weekdays: List<WeekdaySpend>,
    /**
     * What was spent in the current month — PAID expenses only.
     *
     * Equal to `DashboardData.monthSpentMinor` by construction: same filter, same month. The two
     * screens print one number, and this is it.
     */
    val monthExpenseMinor: Long,
    /** Income recorded in the current month. */
    val monthIncomeMinor: Long,
    /**
     * The part of [monthExpenseMinor] that no category claims — either because the transaction
     * carries none, or because the category it points at is gone. Kept as its own figure so the
     * breakdown can state the remainder instead of quietly dropping it.
     */
    val monthUncategorizedMinor: Long,
    val categoriesById: Map<String, Category>,
    val currencyCode: String,
) {
    companion object {
        val Empty = AnalyticsData(
            months = emptyList(),
            monthCategoryBreakdown = emptyList(),
            weekdays = emptyList(),
            monthExpenseMinor = 0,
            monthIncomeMinor = 0,
            monthUncategorizedMinor = 0,
            categoriesById = emptyMap(),
            currencyCode = "PEN",
        )
    }
}

/** Expense and income totals for a single calendar month. */
data class MonthlyPoint(
    val month: YearMonth,
    val expenseMinor: Long,
    val incomeMinor: Long,
)

/** How much was spent on a given weekday, and how many transactions landed on it. */
data class WeekdaySpend(
    val dayOfWeek: DayOfWeek,
    val amountMinor: Long,
    val count: Int,
)
