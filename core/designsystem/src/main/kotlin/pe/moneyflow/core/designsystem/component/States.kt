package pe.moneyflow.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.illustration.Illustration
import pe.moneyflow.core.designsystem.illustration.MoneyFlowIllustration
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing

/** Friendly empty-state block: large icon chip, title and supporting text. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    StateBlock(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        art = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.xl),
                )
            }
        },
    )
}

/**
 * Empty state with a drawn illustration instead of an icon chip.
 *
 * Preferred wherever the empty state is the *whole screen* — a first-run or cleared-out list is a moment
 * worth a real drawing rather than a grey glyph. The icon-chip variant above stays for empty states
 * nested inside a card next to other content, where an illustration would overpower.
 */
@Composable
fun EmptyState(
    illustration: Illustration,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    StateBlock(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        art = { MoneyFlowIllustration(illustration = illustration, size = 104.dp) },
    )
}

/**
 * The counterpart to [EmptyState] for when something *failed* rather than simply not existing yet.
 *
 * This distinction is the whole point. The app previously had no error UI at all outside the lock
 * screen — a failed load fell through to an empty state, so the user was told "no tienes gastos"
 * when the truth was "we couldn't read your gastos." One of those invites a shrug; the other invites
 * panic about lost data. The error chip is tinted with `errorContainer` so it reads as a problem, and
 * [onRetry] gives the user something to do about it.
 *
 * Keep [subtitle] reassuring: say the data couldn't be *loaded*, never that it's missing.
 */
@Composable
fun ErrorState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    retryLabel: String = "Reintentar",
    onRetry: (() -> Unit)? = null,
) {
    StateBlock(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        art = {
            MoneyFlowIllustration(
                illustration = Illustration.LoadFailed,
                size = 104.dp,
                // Tinted from `error` rather than the brand, so it reads as a problem at a glance.
                accent = MaterialTheme.colorScheme.error,
            )
        },
    ) {
        if (onRetry != null) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.chip),
                )
                Text(text = retryLabel, modifier = Modifier.padding(start = Spacing.sm))
            }
        }
    }
}

/** Shared anatomy for [EmptyState] and [ErrorState] so the two stay visually consistent. */
@Composable
private fun StateBlock(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        art()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.lg),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        action()
    }
}

/** How wide the travelling highlight is. Fixed rather than size-relative so every skeleton on a
 *  screen sweeps at the same visual speed regardless of how large it is. */
private val ShimmerSweep = 220.dp

/**
 * A shimmering placeholder box used to build loading skeletons.
 *
 * This is a real gradient sweep. It used to animate *alpha* on a flat `surfaceVariant`, which reads
 * as a pulse — the whole block breathing in unison — rather than as light travelling across a
 * surface. The sweep is what makes a skeleton feel like content arriving instead of a placeholder
 * blinking.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    // The highlight leans toward `surface`, so it lightens in light mode and lifts in dark mode
    // without ever hardcoding white.
    val highlight = lerp(base, MaterialTheme.colorScheme.surface, 0.6f)
    val sweepPx = with(LocalDensity.current) { ShimmerSweep.toPx() }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -sweepPx,
        targetValue = sweepPx * 3f,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing)),
        label = "shimmer-offset",
    )
    // Clamped at both ends, so everything outside the travelling band stays the base color.
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + sweepPx, sweepPx),
    )
    Box(modifier = modifier.clip(shape).drawBehind { drawRect(brush) })
}
