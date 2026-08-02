package pe.moneyflow.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Motion

/**
 * The app's progress bar, for budget and savings-goal fill.
 *
 * Animates to [fraction] rather than jumping to it. The sweep is not decoration: watching a bar fill
 * is what makes the *proportion* legible, whereas a bar that simply appears at its final width reads
 * as a static width. It also makes change visible — after logging an expense you see the budget move,
 * instead of having to compare against a number you no longer remember.
 *
 * Uses [Motion.progress], which is deliberately overshoot-free: a bar that bounces past its value and
 * settles back would, for a moment, misreport an overspend.
 */
@Composable
fun MoneyProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = Motion.progress(),
        label = "progress-fill",
    )
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50)),
        color = color,
        trackColor = trackColor,
    )
}
