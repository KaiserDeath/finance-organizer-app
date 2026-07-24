package pe.moneyflow.core.model

import java.time.LocalDate

/** A spending limit for a category (or overall, when [categoryId] is null) over a period. */
data class Budget(
    val id: String,
    val name: String,
    val categoryId: String? = null,
    val amountMinor: Long,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: LocalDate = LocalDate.now(),
    val rollover: Boolean = false,
    val currencyCode: String = "PEN",
)
