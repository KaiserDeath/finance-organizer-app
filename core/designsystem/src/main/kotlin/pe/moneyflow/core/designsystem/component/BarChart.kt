package pe.moneyflow.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Motion

/** One column in a [BarChart]: a non-negative [value], its axis [label] and highlight state. */
data class BarChartEntry(
    val label: String,
    val value: Long,
    val highlighted: Boolean = false,
)

/**
 * A lightweight, dependency-free vertical bar chart drawn with plain layout composables (no
 * Canvas needed since bars are rectangles). Bars are scaled against the largest value; a value
 * of zero still shows a faint track so the axis stays readable.
 *
 * [contentDescription] is **required, not optional**. The bars are plain `Box`es with no text, so
 * without it a screen-reader user gets the axis labels and no values whatsoever — the chart is
 * simply absent. Making it a required parameter means a new chart cannot ship undescribed; that's
 * worth more than a convention nobody remembers. Pass [valueLabel] to have each bar's value read
 * out too, which is what turns the summary into actual data.
 */
@Composable
fun BarChart(
    entries: List<BarChartEntry>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    valueLabel: ((BarChartEntry) -> String)? = null,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    barHeight: Dp = 140.dp,
) {
    val maxValue = entries.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    // Collapsed into a single focus stop reading the whole series. A per-bar node per column would
    // make the user swipe through decorative boxes to reach each number.
    val spokenDescription = if (valueLabel == null) {
        contentDescription
    } else {
        contentDescription + " " +
            entries.joinToString(", ") { "${it.label} ${valueLabel(it)}" } + "."
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { this.contentDescription = spokenDescription },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEach { entry ->
            val target = (entry.value.toFloat() / maxValue).coerceIn(0f, 1f)
            // Grow each bar to its value instead of appearing at full height. Animating the fraction
            // per bar (rather than a shared progress) means a single changed month re-grows on its own
            // while the rest hold steady, which reads as "this one changed".
            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = Motion.progress(),
                label = "bar-${entry.label}",
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Faint full-height track behind each bar.
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(trackColor.copy(alpha = 0.35f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .width(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (entry.highlighted) highlightColor else barColor),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 1.dp),
                )
            }
        }
    }
}
