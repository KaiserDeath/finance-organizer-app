package pe.moneyflow.core.model

import java.time.LocalDate

/**
 * A savings target the user works towards (an emergency fund, a trip…). [currentAmountMinor]
 * is what has been set aside so far; contributions add to it. Optionally linked to an
 * [accountId] where the money physically lives.
 */
data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long = 0,
    val targetDate: LocalDate? = null,
    val accountId: String? = null,
    val colorHex: String = "#4DB6AC",
    val iconKey: String = "savings",
) {
    /** Progress in the 0f..1f range, clamped. */
    val fraction: Float
        get() = if (targetAmountMinor > 0) {
            (currentAmountMinor.toFloat() / targetAmountMinor).coerceIn(0f, 1f)
        } else {
            0f
        }

    val percent: Int get() = (fraction * 100).toInt()

    val isComplete: Boolean get() = targetAmountMinor > 0 && currentAmountMinor >= targetAmountMinor

    /** How much is still needed to reach the target (never negative). */
    val remainingMinor: Long get() = (targetAmountMinor - currentAmountMinor).coerceAtLeast(0)
}
