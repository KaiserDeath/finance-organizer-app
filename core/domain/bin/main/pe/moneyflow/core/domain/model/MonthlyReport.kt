package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Category
import java.time.YearMonth

/**
 * A single month's spending compared against the month before it, computed by
 * [pe.moneyflow.core.domain.usecase.GetMonthlyReportUseCase]. Amounts are minor units.
 */
data class MonthlyReport(
    val month: YearMonth,
    val currentExpenseMinor: Long,
    val previousExpenseMinor: Long,
    val currentIncomeMinor: Long,
    val previousIncomeMinor: Long,
    val transactionCount: Int,
    /** Per-category expense change vs the previous month, biggest current spend first. */
    val categoryDeltas: List<CategoryDelta>,
    val currencyCode: String,
) {
    /** Signed change in month expense (positive = spent more than last month). */
    val expenseDeltaMinor: Long get() = currentExpenseMinor - previousExpenseMinor

    /** Change as a fraction of last month's expense, or null when there's nothing to compare. */
    val expenseDeltaFraction: Float?
        get() = if (previousExpenseMinor > 0) {
            expenseDeltaMinor.toFloat() / previousExpenseMinor
        } else {
            null
        }

    val balanceMinor: Long get() = currentIncomeMinor - currentExpenseMinor

    companion object {
        fun empty(month: YearMonth, currencyCode: String = "PEN") = MonthlyReport(
            month = month,
            currentExpenseMinor = 0,
            previousExpenseMinor = 0,
            currentIncomeMinor = 0,
            previousIncomeMinor = 0,
            transactionCount = 0,
            categoryDeltas = emptyList(),
            currencyCode = currencyCode,
        )
    }
}

/** One category's expense this month next to the same category last month. */
data class CategoryDelta(
    val category: Category,
    val currentMinor: Long,
    val previousMinor: Long,
) {
    val deltaMinor: Long get() = currentMinor - previousMinor
}
