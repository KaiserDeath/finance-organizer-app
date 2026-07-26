package pe.moneyflow.core.domain.usecase

import pe.moneyflow.core.model.ExchangeRate

/**
 * Converts money between currencies using a set of known [ExchangeRate]s. Resolution order for a
 * `from -> to` rate: identity, a direct rate, an inverse rate, then a single pivot currency
 * (e.g. USD -> PEN -> EUR). Returns null when no path exists, so callers can flag the gap rather
 * than silently assuming 1:1.
 */
object CurrencyConverter {

    /** The multiplier to turn an amount in [from] into [to], or null if it cannot be derived. */
    fun rate(from: String, to: String, rates: List<ExchangeRate>): Double? {
        if (from.equals(to, ignoreCase = true)) return 1.0
        directOrInverse(from, to, rates)?.let { return it }

        // Try one intermediate ("pivot") currency present in the rate table.
        val currencies = rates.flatMap { listOf(it.base, it.quote) }
            .distinctBy { it.uppercase() }
        for (pivot in currencies) {
            if (pivot.equals(from, true) || pivot.equals(to, true)) continue
            val first = directOrInverse(from, pivot, rates) ?: continue
            val second = directOrInverse(pivot, to, rates) ?: continue
            return first * second
        }
        return null
    }

    /** Converts [amountMinor] from currency [from] to [to] in minor units, or null if unknown. */
    fun convert(amountMinor: Long, from: String, to: String, rates: List<ExchangeRate>): Long? =
        rate(from, to, rates)?.let { Math.round(amountMinor * it) }

    private fun directOrInverse(from: String, to: String, rates: List<ExchangeRate>): Double? {
        rates.firstOrNull { it.base.equals(from, true) && it.quote.equals(to, true) }
            ?.let { return it.rate }
        rates.firstOrNull { it.base.equals(to, true) && it.quote.equals(from, true) && it.rate != 0.0 }
            ?.let { return 1.0 / it.rate }
        return null
    }
}
