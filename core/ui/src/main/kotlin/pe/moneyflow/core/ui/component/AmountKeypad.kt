package pe.moneyflow.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.pressScale
import pe.moneyflow.core.designsystem.theme.Spacing

/** The decimal separator [pe.moneyflow.core.common.Money] parses. */
private const val DecimalSeparator = '.'
private const val MaxDecimals = 2

/**
 * The read-out half of the keypad pattern: the amount as a headline, tappable to bring the keypad
 * back. Deliberately **not** a text field — there is no IME here, so the keypad is the only way to
 * change the value and the two can never disagree about what was typed.
 *
 * Lives beside [AmountKeypad] because they are always used together: display on top, keypad below,
 * both fed by the same string. It started private inside add/edit and moved here when the budget
 * sheets needed the same control — entering an amount should not look like two different jobs
 * depending on which screen you are standing on.
 *
 * Never masked by discreet mode: you are entering this number, so hiding it makes the control
 * unusable rather than private.
 */
@Composable
fun AmountDisplay(
    amountText: String,
    currencyCode: String,
    modifier: Modifier = Modifier,
    label: String = "Monto",
    /**
     * What tapping the read-out does — normally "bring the keypad back". Null where the keypad is
     * always on screen and there is nothing to reveal: a tap target that does nothing still
     * announces itself as a button to TalkBack, which is a promise the control cannot keep.
     */
    onClick: (() -> Unit)? = null,
) {
    val hasValue = amountText.isNotEmpty()
    Column(
        modifier = modifier
            // The whole read-out is the target, not just the glyphs.
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick, role = Role.Button).heightIn(min = 48.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${Money.symbolFor(currencyCode)} ${if (hasValue) amountText else "0"}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (hasValue) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * An in-app numeric keypad for money entry, open from the start instead of waiting for the IME.
 *
 * Editing rules are enforced here rather than validated after the fact: at most one separator,
 * at most two decimals, and no leading zero pile-up — so the field can never hold something the
 * money parser would reject.
 */
@Composable
fun AmountKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(DecimalSeparator.toString(), "0", BACKSPACE),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        enabled = isKeyEnabled(key, value),
                        onClick = { onValueChange(apply(key, value)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    key: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    // A disabled key must not look tappable: the container dims, the label keeps full contrast,
    // and there is no ripple to suggest it did something.
    val container = if (enabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val content = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        interactionSource = interaction,
        modifier = modifier
            .heightIn(min = 52.dp)
            .pressScale(interaction)
            .semantics { contentDescription = if (key == BACKSPACE) "Borrar" else key },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (key == BACKSPACE) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null)
            } else {
                Text(text = key, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

private const val BACKSPACE = "⌫"

private fun isKeyEnabled(key: String, value: String): Boolean = when {
    key == BACKSPACE -> value.isNotEmpty()
    key == DecimalSeparator.toString() -> !value.contains(DecimalSeparator)
    // Digits stop once the decimals are full.
    else -> value.substringAfter(DecimalSeparator, "").length < MaxDecimals ||
        !value.contains(DecimalSeparator)
}

/** The value after pressing [key]; returns [value] unchanged when the press isn't allowed. */
internal fun apply(key: String, value: String): String = when {
    key == BACKSPACE -> value.dropLast(1)
    !isKeyEnabled(key, value) -> value
    key == DecimalSeparator.toString() -> if (value.isEmpty()) "0$key" else value + key
    // "0" only leads when a separator follows it, so "007" can't happen.
    value == "0" -> key
    else -> value + key
}
