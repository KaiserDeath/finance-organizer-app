package pe.moneyflow.core.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.PositiveGreen
import pe.moneyflow.core.model.TransactionType

/**
 * Formats a minor-unit amount for display, signed and colored by [type]
 * (income shows "+" in green, expense shows "−").
 */
@Composable
fun AmountText(
    amountMinor: Long,
    currencyCode: String,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    showSign: Boolean = true,
) {
    val formatted = Money.format(amountMinor, currencyCode)
    val sign = when {
        !showSign -> ""
        type == TransactionType.INCOME -> "+ "
        type == TransactionType.EXPENSE -> "− "
        else -> ""
    }
    val color = when (type) {
        TransactionType.INCOME -> PositiveGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = "$sign$formatted",
        style = style.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = modifier,
    )
}
