package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.Account

/**
 * An [account] paired with its computed current balance in the account's own currency.
 * [currentBalanceMinor] = opening balance + income - expenses - transfers out + transfers in
 * (paid movements only). [convertedMinor] is that balance expressed in the user's base currency
 * for net-worth aggregation, or null when no conversion rate is available.
 */
data class AccountBalance(
    val account: Account,
    val currentBalanceMinor: Long,
    val convertedMinor: Long?,
)
