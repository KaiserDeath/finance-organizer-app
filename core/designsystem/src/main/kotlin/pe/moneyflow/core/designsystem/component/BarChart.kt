package pe.moneyflow.core.designsystem.component

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
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 */
@Composable
fun BarChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    barHeight: Dp = 140.dp,
) {
    val maxValue = entries.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEach { entry ->
            val fraction = (entry.value.toFloat() / maxValue).coerceIn(0f, 1f)
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
