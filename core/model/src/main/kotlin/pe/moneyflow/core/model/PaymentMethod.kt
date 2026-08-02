package pe.moneyflow.core.model

/**
 * How a payment is made (Cash, Yape, a specific card/bank…).
 *
 * [deepLinkPackage] is the Android package id of the method's official app, used to launch it
 * directly; [playStoreId] is the fallback package to open on the Play Store when the app is not
 * installed. Both are optional and, for Peru bank apps, should be verified before release.
 */
data class PaymentMethod(
    val id: String,
    val name: String,
    val type: PaymentMethodType = PaymentMethodType.CASH,
    /** For [PaymentMethodType.CARD], whether it's a debit or credit card; null otherwise. */
    val cardKind: CardKind? = null,
    val iconKey: String,
    val colorHex: String,
    val accountId: String? = null,
    val deepLinkPackage: String? = null,
    val playStoreId: String? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)
