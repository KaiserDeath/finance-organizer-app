package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction
import java.time.YearMonth

/** Everything the Dashboard shows, computed by [pe.moneyflow.core.domain.usecase.GetDashboardUseCase]. */
data class DashboardData(
    /** The month this snapshot describes — not necessarily the current one. */
    val month: YearMonth,
    val monthSpentMinor: Long,
    val todaySpentMinor: Long,
    val monthIncomeMinor: Long,
    /** Number of transactions recorded in [month]. */
    val monthTransactionCount: Int = 0,
    /** Expense total for the month before [month], so the hero can show a real comparison. */
    val previousMonthSpentMinor: Long = 0,
    /** The single biggest expense in [month] — a high-signal, one-line fact. */
    val largestExpense: Transaction? = null,
    val recent: List<Transaction>,
    val categoryBreakdown: List<CategorySpend>,
    /** Lookup so recent rows can resolve their category without another query. */
    val categoriesById: Map<String, Category>,
    val currencyCode: String,
) {
    /** Signed change vs the previous month; positive means more was spent. */
    val spendDeltaMinor: Long get() = monthSpentMinor - previousMonthSpentMinor

    /** Change as a fraction of the previous month, or null when there's nothing to compare against. */
    val spendDeltaFraction: Float?
        get() = if (previousMonthSpentMinor > 0) {
            spendDeltaMinor.toFloat() / previousMonthSpentMinor
        } else {
            null
        }

    val balanceMinor: Long get() = monthIncomeMinor - monthSpentMinor

    companion object {
        fun empty(month: YearMonth, currencyCode: String = "PEN") = DashboardData(
            month = month,
            monthSpentMinor = 0,
            todaySpentMinor = 0,
            monthIncomeMinor = 0,
            recent = emptyList(),
            categoryBreakdown = emptyList(),
            categoriesById = emptyMap(),
            currencyCode = currencyCode,
        )
    }
}

/** How much was spent in one category this month, and its share of total spend (0f..1f). */
data class CategorySpend(
    val category: Category,
    val amountMinor: Long,
    val fraction: Float,
)
