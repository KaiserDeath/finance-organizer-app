package pe.moneyflow.core.model

/**
 * A one-tap expense preset for the dashboard's "De un toque" row: tapping it saves a movement
 * with this description, amount, category and method in a single gesture.
 *
 * Sourced from onboarding, or inferred from the most frequent description+category+method
 * combinations once there is enough history.
 */
data class QuickShortcut(
    val label: String,
    val amountMinor: Long,
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
)
