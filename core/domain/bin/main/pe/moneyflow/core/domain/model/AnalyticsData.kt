package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Everything the Analytics screen shows over a rolling window of months, computed by
 * [pe.moneyflow.core.domain.usecase.GetAnalyticsUseCase]. Amounts are minor units (céntimos).
 */
data class AnalyticsData(
    /** One entry per calendar month in the window, oldest first. */
    val months: List<MonthlyPoint>,
    /** Expense totals per category across the whole window, largest first. */
    val categoryBreakdown: List<CategorySpend>,
    /** Expense totals per weekday (Mon..Sun) across the window. */
    val weekdays: List<WeekdaySpend>,
    val totalExpenseMinor: Long,
    val totalIncomeMinor: Long,
    /** Average daily expense across the days actually covered by the window. */
    val avgDailyExpenseMinor: Long,
    val categoriesById: Map<String, Category>,
    val currencyCode: String,
) {
    companion object {
        val Empty = AnalyticsData(
            months = emptyList(),
            categoryBreakdown = emptyList(),
            weekdays = emptyList(),
            totalExpenseMinor = 0,
            totalIncomeMinor = 0,
            avgDailyExpenseMinor = 0,
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
