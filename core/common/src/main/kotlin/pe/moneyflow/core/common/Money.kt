package pe.moneyflow.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money helpers. Everything is expressed in **minor units** (céntimos) as [Long] so arithmetic
 * stays exact; formatting/parsing to human decimals happens only here, at the UI edge.
 */
object Money {

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        // Peru convention: comma thousands separator, dot decimal separator.
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    fun symbolFor(currencyCode: String): String = when (currencyCode.uppercase(Locale.US)) {
        "PEN" -> "S/"
        "USD" -> "$"
        "EUR" -> "€"
        else -> currencyCode.uppercase(Locale.US)
    }

    /**
     * The discreet-mode stand-in for an amount, e.g. "S/ ••••••".
     *
     * Fixed width on purpose. A mask whose length tracked the number of digits would still tell a
     * reader across the table whether you are looking at hundreds or tens of thousands, which is
     * most of what the mask exists to withhold. The currency symbol stays: it identifies nothing
     * and it keeps the row recognisable as money.
     */
    fun mask(currencyCode: String = "PEN"): String = "${symbolFor(currencyCode)} ••••••"

    /** [format], or [mask] when discreet mode is on. The UI edge decides; nothing upstream changes. */
    fun format(amountMinor: Long, currencyCode: String = "PEN", hidden: Boolean): String =
        if (hidden) mask(currencyCode) else format(amountMinor, currencyCode)

    /** e.g. 123456 -> "S/ 1,234.56". */
    fun format(amountMinor: Long, currencyCode: String = "PEN"): String {
        val amount = BigDecimal(amountMinor).movePointLeft(2)
        return "${symbolFor(currencyCode)} ${decimalFormat.format(amount)}"
    }

    /** Format without the currency symbol, e.g. "1,234.56". */
    fun formatPlain(amountMinor: Long): String =
        decimalFormat.format(BigDecimal(amountMinor).movePointLeft(2))

    /**
     * Parse free-form user input ("12", "12.5", "1,234.56", "S/ 20") into minor units.
     * Returns null when the input is not a valid amount.
     */
    fun parseToMinor(input: String): Long? {
        val cleaned = input
            .replace(Regex("[^0-9.,-]"), "")
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
        return runCatching {
            BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        }.getOrNull()
    }
}
