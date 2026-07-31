package pe.moneyflow.core.ui.preset

import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.CardKind
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
    /**
     * Play Store package id of the method's official Android app, used to deep-link into it. Null
     * for options with no dedicated app (Efectivo, generic card, and Plin — which lives inside the
     * bank apps rather than shipping its own). Verified against Google Play, July 2026.
     */
    val deepLinkPackage: String? = null,
    /**
     * Official mobile web-banking URL, opened in an external browser as a fallback when the app is
     * not installed. Banks only; wallets (Yape/Plin) and cash have none. Verified July 2026.
     */
    val webUrl: String? = null,
    /** Debit/credit nature for card presets; null for cash, wallets and banks. */
    val cardKind: CardKind? = null,
)

object FinancePresets {
    val all: List<FinancePreset> = listOf(
        FinancePreset("Efectivo", AccountType.CASH, PaymentMethodType.CASH, "cash", "#66BB6A"),
        FinancePreset("BCP", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#EA6A1E", "com.bcp.bank.bcp", "https://bcpzonasegura.viabcp.com/"),
        FinancePreset("BBVA", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#1464A5", "com.bbva.nxt_peru", "https://www.bbva.pe/personas/acceso-banca-por-internet.html"),
        FinancePreset("Interbank", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#00A94F", "pe.com.interbank.mobilebanking", "https://bancaporinternet.interbank.pe/"),
        FinancePreset("Scotiabank", AccountType.BANK, PaymentMethodType.BANK, "account_balance", "#D5122B", "pe.com.scotiabank.blpm.android.client", "https://www.scotiabank.com.pe/Personas/Canales-digitales/canales/banca-por-internet"),
        FinancePreset("Yape", AccountType.EWALLET, PaymentMethodType.EWALLET, "wallet", "#742284", "com.bcp.innovacxion.yapeapp"),
        FinancePreset("Plin", AccountType.EWALLET, PaymentMethodType.EWALLET, "wallet", "#00B7C4"),
        FinancePreset("Tunki", AccountType.EWALLET, PaymentMethodType.EWALLET, "wallet", "#00C2A8"),
        FinancePreset("Tarjeta de débito", AccountType.BANK, PaymentMethodType.CARD, "card", "#42A5F5", cardKind = CardKind.DEBIT),
        FinancePreset("Tarjeta de crédito", AccountType.CREDIT_CARD, PaymentMethodType.CARD, "card", "#EF5350", cardKind = CardKind.CREDIT),
    )

    /** Official web-banking URL for a stored [packageName], so it need not be persisted per method. */
    fun webUrlFor(packageName: String?): String? =
        packageName?.let { pkg -> all.firstOrNull { it.deepLinkPackage == pkg }?.webUrl }
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

/**
 * Account type for a payment method, honoring the card's [cardKind] so a **debit** card links to a
 * Bank (checking) account and a **credit** card to a Credit-card account. Falls back to
 * [toAccountType] for non-cards.
 */
fun accountTypeFor(type: PaymentMethodType, cardKind: CardKind?): AccountType = when (cardKind) {
    CardKind.DEBIT -> AccountType.BANK
    CardKind.CREDIT -> AccountType.CREDIT_CARD
    null -> type.toAccountType()
}
