package pe.moneyflow.core.domain.model

/**
 * Aggregate financial position: every account balance converted into a single [currencyCode].
 * [assetsMinor] sums positive balances, [liabilitiesMinor] the negative ones (e.g. credit-card
 * debt); [totalMinor] is their net. [hasUnconvertible] flags that some account used a currency
 * with no known rate, so the total omits it.
 */
data class NetWorth(
    val currencyCode: String,
    val totalMinor: Long,
    val assetsMinor: Long,
    val liabilitiesMinor: Long,
    val balances: List<AccountBalance>,
    val hasUnconvertible: Boolean,
)
