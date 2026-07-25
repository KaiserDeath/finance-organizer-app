package pe.moneyflow.feature.accounts

import pe.moneyflow.core.model.AccountType

/** Default icon/color/label for each account type, so the editor stays simple. */
internal data class AccountPreset(
    val label: String,
    val iconKey: String,
    val colorHex: String,
)

internal object AccountPresets {
    fun of(type: AccountType): AccountPreset = when (type) {
        AccountType.CASH -> AccountPreset("Efectivo", "cash", "#66BB6A")
        AccountType.BANK -> AccountPreset("Banco", "account_balance", "#42A5F5")
        AccountType.CREDIT_CARD -> AccountPreset("Tarjeta", "card", "#EF5350")
        AccountType.EWALLET -> AccountPreset("Billetera", "wallet", "#AB47BC")
        AccountType.SAVINGS -> AccountPreset("Ahorro", "savings", "#4DB6AC")
    }

    val ordered: List<AccountType> = listOf(
        AccountType.CASH,
        AccountType.BANK,
        AccountType.CREDIT_CARD,
        AccountType.EWALLET,
        AccountType.SAVINGS,
    )
}

/** Currencies the editor offers out of the box. */
internal val supportedCurrencies = listOf("PEN", "USD", "EUR")
