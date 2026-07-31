package pe.moneyflow.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Motion
import kotlin.math.abs
import kotlin.math.roundToInt

/** A point the user has scrubbed to, reported so the host can render its own readout. */
data class LineChartSelection(
    /** Zero-based day index within the current series. */
    val index: Int,
    val currentMinor: Long,
    val previousMinor: Long?,
)

/**
 * Two cumulative curves — this month against last — with drag-to-scrub.
 *
 * Hand-drawn on a [Canvas] rather than delegating to a charting library, deliberately:
 *
 *  - The series here are small and fixed (28–31 points, no scroll or zoom), so a library's main
 *    advantages don't apply.
 *  - Charts in this app are required to be described for screen readers — `contentDescription` is a
 *    *mandatory* parameter on every chart precisely because half of them were previously invisible to
 *    TalkBack. A third-party chart bypasses that enforcement at the exact spot it was added to protect.
 *  - Drawing it here keeps the chart inside the design system's vocabulary: brand colors, the shared
 *    [Motion] specs, and the same stroke and corner language as the other charts.
 *
 * The previous month is dashed and de-emphasised so the two lines are distinguishable without relying
 * on colour alone — the same reasoning behind the `+`/`−` prefixes on amounts.
 */
@Composable
fun CumulativeLineChart(
    current: List<Long>,
    previous: List<Long>,
    peakMinor: Long,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    previousColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onSelectionChange: (LineChartSelection?) -> Unit = {},
) {
    val density = LocalDensity.current
    val strokePx = with(density) { 2.5.dp.toPx() }
    val previousStrokePx = with(density) { 1.5.dp.toPx() }
    val dotRadiusPx = with(density) { 4.dp.toPx() }

    // Draw-in, same rationale as the other charts: a curve that grows is understood as it forms.
    val growth = remember { Animatable(0f) }
    LaunchedEffect(current.size, peakMinor) {
        growth.snapTo(0f)
        growth.animateTo(1f, animationSpec = Motion.progress())
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val fillBrush = Brush.verticalGradient(
        listOf(lineColor.copy(alpha = 0.22f), lineColor.copy(alpha = 0f)),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(current.size) {
                if (current.size < 2) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val i = indexFor(offset.x, size.width, current.size)
                        selectedIndex = i
                        onSelectionChange(selectionAt(i, current, previous))
                        // Hold the readout while the finger is down, clear it on release.
                        tryAwaitRelease()
                        selectedIndex = null
                        onSelectionChange(null)
                    },
                )
            }
            .pointerInput(current.size) {
                if (current.size < 2) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        selectedIndex = null
                        onSelectionChange(null)
                    },
                    onDragCancel = {
                        selectedIndex = null
                        onSelectionChange(null)
                    },
                ) { change, _ ->
                    val i = indexFor(change.position.x, size.width, current.size)
                    if (i != selectedIndex) {
                        selectedIndex = i
                        onSelectionChange(selectionAt(i, current, previous))
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (current.size < 2 || peakMinor <= 0L) return@Canvas
            val ceiling = peakMinor.toFloat()

            // Previous month spans its own full length so the curves stay comparable at any x, but is
            // mapped onto the same x-range as the current series.
            if (previous.size >= 2) {
                drawSeries(
                    values = previous,
                    ceiling = ceiling,
                    color = previousColor.copy(alpha = 0.55f),
                    strokeWidth = previousStrokePx,
                    progress = growth.value,
                    dashed = true,
                )
            }
            drawSeries(
                values = current,
                ceiling = ceiling,
                color = lineColor,
                strokeWidth = strokePx,
                progress = growth.value,
                fillBrush = fillBrush,
            )

            selectedIndex?.let { i ->
                val x = xFor(i, current.size, size.width)
                drawLine(
                    color = lineColor.copy(alpha = 0.35f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = previousStrokePx,
                )
                val y = yFor(current[i].toFloat(), ceiling, size.height)
                drawCircle(color = lineColor, radius = dotRadiusPx, center = Offset(x, y))
            }
        }
    }
}

private fun indexFor(x: Float, width: Int, count: Int): Int {
    if (count <= 1) return 0
    val fraction = (x / width.toFloat()).coerceIn(0f, 1f)
    return (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

private fun selectionAt(index: Int, current: List<Long>, previous: List<Long>) =
    LineChartSelection(
        index = index,
        currentMinor = current.getOrElse(index) { 0L },
        previousMinor = previous.getOrNull(index),
    )

private fun xFor(index: Int, count: Int, width: Float): Float =
    if (count <= 1) 0f else width * index / (count - 1).toFloat()

private fun yFor(value: Float, ceiling: Float, height: Float): Float {
    // Inset top and bottom so a peak or a zero doesn't sit flush against the edge.
    val usable = height * 0.9f
    val top = height * 0.05f
    val fraction = if (ceiling <= 0f) 0f else (value / ceiling).coerceIn(0f, 1f)
    return top + usable * (1f - fraction)
}

/** Draws one series as a polyline, optionally dashed and optionally filled beneath. */
private fun DrawScope.drawSeries(
    values: List<Long>,
    ceiling: Float,
    color: Color,
    strokeWidth: Float,
    progress: Float,
    dashed: Boolean = false,
    fillBrush: Brush? = null,
) {
    // Reveal left-to-right by drawing only the leading portion of the series.
    val visibleCount = (values.size * progress).roundToInt().coerceIn(2, values.size)
    val points = (0 until visibleCount).map { i ->
        Offset(
            x = xFor(i, values.size, size.width),
            y = yFor(values[i].toFloat(), ceiling, size.height),
        )
    }
    if (points.size < 2) return

    val line = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }

    if (fillBrush != null) {
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(path = area, brush = fillBrush)
    }

    drawPath(
        path = line,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 3, strokeWidth * 3))
            } else {
                null
            },
        ),
    )
}

/** Formats a scrub readout as "day N" plus both values, for the host's label row. */
fun LineChartSelection.dayLabel(): String = "Día ${index + 1}"

/** Signed difference at the scrubbed point, or null when there's no comparison available. */
fun LineChartSelection.deltaMinor(): Long? =
    previousMinor?.let { currentMinor - it }

/** Absolute delta, for formatting alongside a direction arrow. */
fun LineChartSelection.absDeltaMinor(): Long? = deltaMinor()?.let { abs(it) }
