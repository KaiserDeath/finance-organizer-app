package pe.moneyflow.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.TabularFigures

/**
 * A currency figure that counts to its value instead of snapping to it.
 *
 * Used for the headline amounts a user watches change — chiefly the dashboard hero. When you log an
 * expense and come back, the number *moves*, so the change is something you see rather than something
 * you'd have to remember the previous value to notice.
 *
 * This is the one place tabular figures genuinely matter for a *single* number rather than a column:
 * without them the text re-flows on every intermediate frame as digit widths change, so the amount
 * visibly jitters as it counts. With `tnum` the width is stable and only the digits move.
 *
 * Deliberately not used for ordinary list amounts: dozens of simultaneously counting rows would be
 * noise, and those values aren't changing in front of the user anyway.
 */
@Composable
fun AnimatedAmount(
    amountMinor: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 2,
) {
    // Animate in major units: minor units would need Long interpolation and the extra precision is
    // invisible at these speeds anyway.
    val animated by animateFloatAsState(
        targetValue = amountMinor / 100f,
        animationSpec = Motion.progress(),
        label = "amount-count",
    )
    Text(
        text = Money.format((animated * 100).toLong(), currencyCode),
        style = style.copy(fontFeatureSettings = TabularFigures),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
