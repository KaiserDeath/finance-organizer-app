package pe.moneyflow.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Motion

/** One arc of a [DonutChart]: its share of the whole (0f..1f) and its color. */
data class DonutSlice(val fraction: Float, val color: Color)

/**
 * A lightweight, dependency-free donut chart drawn on a [Canvas]. Used for the dashboard's
 * category breakdown. (Richer charts arrive with the analytics phase via Vico.)
 *
 * [contentDescription] is **required, not optional**. It used to be nullable, and the result was
 * exactly what you'd predict: the dashboard passed one and the analytics screen — rendering the same
 * chart from the same data — passed nothing, so half the app's spending breakdowns were invisible to
 * TalkBack. A required parameter removes the opportunity.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    strokeWidth: Dp = 24.dp,
    gapDegrees: Float = 3f,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    centerContent: @Composable () -> Unit = {},
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }

    // Sweep the ring in on first composition, and re-sweep whenever the data changes. The growth is
    // what makes the proportions legible — a ring that simply appears fully drawn has to be *read*,
    // whereas one that fills is understood while it happens. Also makes the chart respond visibly
    // when a new expense shifts the split.
    val sweepProgress = remember { Animatable(0f) }
    val slicesKey = slices.map { it.fraction }
    LaunchedEffect(slicesKey) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(1f, animationSpec = Motion.progress())
    }
    val progress = sweepProgress.value

    val chartModifier = modifier
        .size(diameter)
        .semantics { this.contentDescription = contentDescription }
    Box(modifier = chartModifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Background track.
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            val visible = slices.filter { it.fraction > 0f }
            var startAngle = -90f
            visible.forEach { slice ->
                val fullSweep = slice.fraction * 360f
                val sweep = (fullSweep - gapDegrees).coerceAtLeast(0.5f)
                // Scaling each arc by the same progress makes the whole ring wipe clockwise from 12
                // o'clock as one motion, rather than each slice growing independently in place.
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                startAngle += fullSweep
            }
        }
        centerContent()
    }
}
