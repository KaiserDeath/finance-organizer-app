package pe.moneyflow.core.ui.preset

import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.PaymentMethodType

/**
 * A ready-made bank / wallet / cash option so users don't have to type an account or payment
 * method from scratch. Shared by the Accounts and Payment-methods editors so a "BCP" or "Yape"
 * looks and links up the same way on both sides.
 *
 * Colors are approximate brand tints for quick recognition, not official assets.
 */
data class FinancePreset(
    val name: String,
    val accountType: AccountType,
    val paymentMethodType: PaymentMethodType,
    val iconKey: String,
    val colorHex: String,
)

object FinancePresets {
    val all: List<FinancePreset> = listOf(
        FinancePreset("Efectivo", AccountType.CASH, PaymentMethodType.CASH, "cash", "#66BB6A"),
        FinancePreset("BCP", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#EA6A1E"),
        FinancePreset("BBVA", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#1464A5"),
        FinancePreset("Interbank", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#00A94F"),
        FinancePreset("Scotiabank", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#D5122B"),
        FinancePreset("Yape", AccountType.EWALLET, PaymentMethodType.EWALLET, "wallet", "#742284"),
        FinancePreset("Plin", AccountType.EWALLET, PaymentMethodType.EWALLET, "wallet", "#00B7C4"),
        FinancePreset("Tarjeta de crédito", AccountType.CREDIT_CARD, PaymentMethodType.CARD, "card", "#EF5350"),
    )
}

/** Sensible default payment-method type for an account type, used when auto-creating the pair. */
fun AccountType.toPaymentMethodType(): PaymentMethodType = when (this) {
    AccountType.CASH -> PaymentMethodType.CASH
    AccountType.BANK -> PaymentMethodType.BANK
    AccountType.CREDIT_CARD -> PaymentMethodType.CARD
    AccountType.EWALLET -> PaymentMethodType.EWALLET
    AccountType.SAVINGS -> PaymentMethodType.BANK
}

/** Sensible default account type for a payment-method type, used when auto-creating the pair. */
fun PaymentMethodType.toAccountType(): AccountType = when (this) {
    PaymentMethodType.CASH -> AccountType.CASH
    PaymentMethodType.CARD -> AccountType.CREDIT_CARD
    PaymentMethodType.EWALLET -> AccountType.EWALLET
    PaymentMethodType.BANK -> AccountType.BANK
}
