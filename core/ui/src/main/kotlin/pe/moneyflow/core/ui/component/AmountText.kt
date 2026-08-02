package pe.moneyflow.core.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import pe.moneyflow.core.designsystem.theme.TabularFigures
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.util.money

/**
 * Formats a minor-unit amount for display, signed and colored by [type]
 * (income shows "+" in green, expense shows "−").
 *
 * Always renders with tabular figures so amounts line up on the decimal point when stacked in a
 * list — this is forced here rather than left to [style], because a caller passing an arbitrary
 * text style shouldn't be able to break column alignment.
 */
@Composable
fun AmountText(
    amountMinor: Long,
    currencyCode: String,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    showSign: Boolean = true,
    color: Color? = null,
) {
    val formatted = money(amountMinor, currencyCode)
    val sign = when {
        !showSign -> ""
        type == TransactionType.INCOME -> "+ "
        type == TransactionType.EXPENSE -> "− "
        else -> ""
    }
    // [color] overrides the default (used to mute not-yet-paid amounts); otherwise income is green.
    val resolvedColor = color ?: when (type) {
        TransactionType.INCOME -> MaterialTheme.moneyColors.positive
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = "$sign$formatted",
        style = style.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = TabularFigures,
        ),
        color = resolvedColor,
        modifier = modifier,
    )
}
