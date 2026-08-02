package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.Category

/** A budget with its current-period spend computed against the limit. */
data class BudgetProgress(
    val budget: Budget,
    /** The category this budget tracks, or null for an overall budget. */
    val category: Category?,
    val spentMinor: Long,
    val currencyCode: String,
) {
    val remainingMinor: Long get() = budget.amountMinor - spentMinor

    /** 0f..1f (clamped) share of the limit already spent. */
    val fraction: Float
        get() = if (budget.amountMinor <= 0) 0f
        else (spentMinor.toFloat() / budget.amountMinor).coerceIn(0f, 1f)

    val percentUsed: Int get() = (fraction * 100).toInt()

    val isOverBudget: Boolean get() = spentMinor > budget.amountMinor

    /** True once spending crosses 80% of the limit (warning threshold). */
    val isNearLimit: Boolean get() = !isOverBudget && fraction >= 0.8f

    /**
     * A budget over a fixed-expense category (rent, utilities…). Its overrun reads differently:
     * the spend can't be cut, so the limit is what needs correcting.
     */
    val isFixedExpense: Boolean get() = category?.isFixed == true
}
