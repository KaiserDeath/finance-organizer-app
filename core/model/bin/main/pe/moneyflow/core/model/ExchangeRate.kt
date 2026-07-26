package pe.moneyflow.core.model

import java.time.LocalDate

/**
 * A currency conversion rate: one unit of [base] equals [rate] units of [quote], as of [asOf].
 * e.g. base = "USD", quote = "PEN", rate = 3.75 means 1 USD = 3.75 PEN.
 */
data class ExchangeRate(
    val id: String,
    val base: String,
    val quote: String,
    val rate: Double,
    val asOf: LocalDate,
)
