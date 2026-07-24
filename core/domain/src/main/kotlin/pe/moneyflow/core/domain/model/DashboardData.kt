package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction

/** Everything the Dashboard shows, computed by [pe.moneyflow.core.domain.usecase.GetDashboardUseCase]. */
data class DashboardData(
    val monthSpentMinor: Long,
    val todaySpentMinor: Long,
    val monthIncomeMinor: Long,
    /** Remaining budget for the month, or null when no budget is configured yet. */
    val remainingBudgetMinor: Long?,
    val recent: List<Transaction>,
    val categoryBreakdown: List<CategorySpend>,
    /** Lookup so recent rows can resolve their category without another query. */
    val categoriesById: Map<String, Category>,
    val currencyCode: String,
) {
    companion object {
        val Empty = DashboardData(
            monthSpentMinor = 0,
            todaySpentMinor = 0,
            monthIncomeMinor = 0,
            remainingBudgetMinor = null,
            recent = emptyList(),
            categoryBreakdown = emptyList(),
            categoriesById = emptyMap(),
            currencyCode = "PEN",
        )
    }
}

/** How much was spent in one category this month, and its share of total spend (0f..1f). */
data class CategorySpend(
    val category: Category,
    val amountMinor: Long,
    val fraction: Float,
)
