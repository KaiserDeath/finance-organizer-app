package pe.moneyflow.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small family of empty-state illustrations, drawn in Compose.
 *
 * Every empty state in the app was a 72dp grey Material icon in a grey circle — the most generic empty
 * state possible, repeated on nine screens, at exactly the moments a user is most likely to bounce
 * (first run, and after clearing everything out).
 *
 * Drawn rather than shipped as assets, for three reasons: they cost nothing in APK size, they take
 * their colours from the live theme so light/dark and any palette change follow automatically, and they
 * can share one visual language by construction — a **two-tone geometric system**, brand primary for
 * the subject and a low-alpha tint for supporting shapes, with consistent 2.5dp rounded strokes.
 *
 * Deliberately abstract. Illustration that tries to be charming ages badly and translates poorly; these
 * read as diagrams of the thing that's missing, which is what an empty state actually needs to convey.
 */
enum class Illustration {
    /** No transactions yet — a receipt with empty lines. */
    NoTransactions,

    /** No budgets — a target with nothing aimed at it. */
    NoBudgets,

    /** Nothing scheduled — a calendar with a clear day. */
    NothingDue,

    /** No categories / nothing to break down — an empty ring. */
    NoBreakdown,

    /** No savings goals — a jar with room to fill. */
    NoSavings,

    /** Something failed to load — a disconnected plug. */
    LoadFailed,
}

/**
 * Renders [illustration] at [size].
 *
 * [accent] carries the subject and defaults to the brand primary; supporting shapes derive from it, so
 * a single colour drives the whole drawing and it stays coherent under any theme.
 */
@Composable
fun MoneyFlowIllustration(
    illustration: Illustration,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val tint = accent.copy(alpha = 0.16f)
    val outline = accent.copy(alpha = 0.9f)
    Canvas(modifier = modifier.size(size)) {
        // Everything is authored against a 100x100 space and scaled, so proportions hold at any size.
        val s = this.size.minDimension / 100f
        val stroke = Stroke(width = 2.5f * s, cap = StrokeCap.Round)
        when (illustration) {
            Illustration.NoTransactions -> drawReceipt(s, tint, outline, stroke)
            Illustration.NoBudgets -> drawTarget(s, tint, outline, stroke)
            Illustration.NothingDue -> drawCalendar(s, tint, outline, stroke)
            Illustration.NoBreakdown -> drawRing(s, tint, outline, stroke)
            Illustration.NoSavings -> drawJar(s, tint, outline, stroke)
            Illustration.LoadFailed -> drawUnplugged(s, tint, outline, stroke)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The drawings. Each is authored in a 0..100 box; `s` scales to the real canvas.
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawReceipt(s: Float, tint: Color, outline: Color, stroke: Stroke) {
    // Paper, with a torn bottom edge — the shape that reads "receipt" fastest.
    val body = Path().apply {
        moveTo(28f * s, 16f * s)
        lineTo(72f * s, 16f * s)
        lineTo(72f * s, 78f * s)
        // Zig-zag tear.
        var x = 72f
        var up = false
        while (x > 28f) {
            x -= 7.33f
            lineTo(x * s, (if (up) 78f else 84f) * s)
            up = !up
        }
        close()
    }
    drawPath(body, tint)
    drawPath(body, outline, style = stroke)
    // Empty content lines: short, uneven, clearly unfilled.
    listOf(30f to 26f, 30f to 38f, 30f to 50f).forEachIndexed { i, (x, y) ->
        val len = listOf(30f, 22f, 26f)[i]
        drawLine(
            color = outline.copy(alpha = 0.45f),
            start = Offset((x + 6f) * s, y * s),
            end = Offset((x + 6f + len) * s, y * s),
            strokeWidth = 2.5f * s,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawTarget(s: Float, tint: Color, outline: Color, stroke: Stroke) {
    val c = Offset(50f * s, 50f * s)
    drawCircle(tint, radius = 32f * s, center = c)
    drawCircle(outline, radius = 32f * s, center = c, style = stroke)
    drawCircle(outline.copy(alpha = 0.6f), radius = 19f * s, center = c, style = stroke)
    drawCircle(outline, radius = 6f * s, center = c)
}

private fun DrawScope.drawCalendar(s: Float, tint: Color, outline: Color, stroke: Stroke) {
    val rect = Rect(Offset(22f * s, 24f * s), Size(56f * s, 54f * s))
    drawRoundRect(
        color = tint,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * s),
    )
    drawRoundRect(
        color = outline,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * s),
        style = stroke,
    )
    // Header rule + two hanging rings.
    drawLine(
        color = outline,
        start = Offset(22f * s, 40f * s),
        end = Offset(78f * s, 40f * s),
        strokeWidth = 2.5f * s,
    )
    listOf(36f, 64f).forEach { x ->
        drawLine(
            color = outline,
            start = Offset(x * s, 18f * s),
            end = Offset(x * s, 30f * s),
            strokeWidth = 2.5f * s,
            cap = StrokeCap.Round,
        )
    }
    // A single clear day, marked rather than filled.
    drawCircle(outline.copy(alpha = 0.5f), radius = 5f * s, center = Offset(50f * s, 60f * s), style = stroke)
}

private fun DrawScope.drawRing(s: Float, tint: Color, outline: Color, stroke: Stroke) {
    val c = Offset(50f * s, 50f * s)
    val ringStroke = Stroke(width = 12f * s, cap = StrokeCap.Round)
    drawCircle(tint, radius = 30f * s, center = c, style = ringStroke)
    // One thin arc present, the rest empty — "nothing to break down yet".
    drawArc(
        color = outline,
        startAngle = -90f,
        sweepAngle = 48f,
        useCenter = false,
        topLeft = Offset(20f * s, 20f * s),
        size = Size(60f * s, 60f * s),
        style = ringStroke,
    )
}

private fun DrawScope.drawJar(s: Float, tint: Color, outline: Color, stroke: Stroke) {
    val body = Rect(Offset(30f * s, 34f * s), Size(40f * s, 48f * s))
    val radius = androidx.compose.ui.geometry.CornerRadius(10f * s)
    drawRoundRect(tint, body.topLeft, body.size, radius)
    drawRoundRect(outline, body.topLeft, body.size, radius, style = stroke)
    // Lid.
    drawLine(
        color = outline,
        start = Offset(26f * s, 30f * s),
        end = Offset(74f * s, 30f * s),
        strokeWidth = 4f * s,
        cap = StrokeCap.Round,
    )
    // A shallow fill line, so the jar reads as "room to fill" rather than broken.
    drawLine(
        color = outline.copy(alpha = 0.45f),
        start = Offset(32f * s, 72f * s),
        end = Offset(68f * s, 72f * s),
        strokeWidth = 2.5f * s,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawUnplugged(s: Float, tint: Color, outline: Color, stroke: Stroke) {
    drawCircle(tint, radius = 32f * s, center = Offset(50f * s, 50f * s))
    // Two ends that don't meet — a gap, not a break.
    drawLine(
        color = outline,
        start = Offset(28f * s, 62f * s),
        end = Offset(45f * s, 45f * s),
        strokeWidth = 4f * s,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = outline,
        start = Offset(55f * s, 35f * s),
        end = Offset(72f * s, 52f * s),
        strokeWidth = 4f * s,
        cap = StrokeCap.Round,
    )
    listOf(Offset(41f * s, 34f * s) to Offset(48f * s, 27f * s), Offset(52f * s, 45f * s) to Offset(59f * s, 38f * s))
        .forEach { (start, end) ->
            drawLine(outline.copy(alpha = 0.6f), start, end, strokeWidth = 2.5f * s, cap = StrokeCap.Round)
        }
    drawCircle(outline, radius = 3f * s, center = Offset(50f * s, 40f * s), style = stroke)
}
