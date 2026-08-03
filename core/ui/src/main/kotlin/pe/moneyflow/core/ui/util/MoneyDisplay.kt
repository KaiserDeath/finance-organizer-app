package pe.moneyflow.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.LocalAmountsHidden
import pe.moneyflow.core.domain.model.MessagePart

/**
 * Formats an amount for display, masked when discreet mode is on.
 *
 * Use this instead of [Money.format] anywhere the result is drawn on screen. `Money.format` is a
 * plain function and cannot see the composition, so a call site that keeps using it silently opts
 * out of the mask — which is exactly the hole the feature cannot afford.
 *
 * Deliberately *not* used for: the amount being typed in the add/edit form (you are entering it),
 * PDF and CSV exports (a masked export is a broken export — and an export is explicitly requested,
 * the same distinction the month-over-month rule draws), and onboarding, which runs before the
 * setting exists.
 */
@Composable
@ReadOnlyComposable
fun money(amountMinor: Long, currencyCode: String = "PEN"): String =
    Money.format(amountMinor, currencyCode, hidden = LocalAmountsHidden.current)

/**
 * Renders an insight's message, formatting each amount through [money] so discreet mode applies.
 *
 * This replaced a regular expression that matched formatted amounts back out of finished prose.
 * That worked, but it coupled the mask to the exact output of `Money.format` — change the
 * separator or the symbol spacing and the mask silently stops matching, leaking the figures it
 * exists to hide, with nothing failing. The domain now hands over the numbers instead.
 */
@Composable
@ReadOnlyComposable
fun insightMessage(parts: List<MessagePart>): String {
    // Read the flag once: joinToString's lambda is not a composable context.
    val hidden = LocalAmountsHidden.current
    return parts.joinToString("") { part ->
        when (part) {
            is MessagePart.Text -> part.value
            is MessagePart.Amount ->
                Money.format(part.amountMinor, part.currencyCode, hidden = hidden)
        }
    }
}

/**
 * True when discreet mode is on, for the callers that have to change more than a number — a
 * remaining-budget line that becomes qualitative, say, rather than a masked figure.
 */
@Composable
@ReadOnlyComposable
fun amountsHidden(): Boolean = LocalAmountsHidden.current
