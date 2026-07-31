package pe.moneyflow.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.HeroTintDark
import pe.moneyflow.core.designsystem.theme.HeroTintLight
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing

/**
 * Rounded surface used as the base container across the app.
 *
 * Content padding is [Spacing.xl] rather than [Spacing.lg]: at the 24dp corner radius of
 * `shapes.large`, a 16dp inset lets the corner arc visually crowd the content, which reads cheap
 * despite the generous rounding. Padding wants to be roughly three-quarters of the radius.
 *
 * **Containment is tonal, not a drop shadow.** This used to default to `shadowElevation = 2.dp` while
 * every dashboard call site passed `0.dp` and analytics kept the default — so the same component read
 * differently on two screens. Current M3 guidance separates an in-flow surface from its background by
 * tone, reserving shadow for things that genuinely float (the FAB, sheets, an app bar over scrolled
 * content). [shadowElevation] is still available for those, but defaults to none.
 *
 * The tone is `surface`, not `surfaceContainerLow`: against this palette's `background` (`#F7F7FB`)
 * the Low step (`#FAFAFD`) is very nearly the same value, so cards would dissolve into the canvas.
 * `surface` gives real separation in both themes, which is the point of the tonal approach.
 *
 * The previous `tonalElevation = 1.dp` is also gone. Because the color *was* `surface`, Material
 * applied a faint `surfaceTint` overlay, leaving every card slightly indigo rather than neutral.
 */
@Composable
fun MoneyCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    shadowElevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color,
        shadowElevation = shadowElevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** A translucent, frosted-looking card for hero/summary surfaces (glassmorphism-lite). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Resolve light/dark by the actual surface luminance so this stays correct under both a
    // forced theme and Material You dynamic color — but the tint itself is a fixed brand color.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isDark) HeroTintDark else HeroTintLight,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
    ) {
        // Spacing.xxl against the 28dp extraLarge radius, for the same reason as MoneyCard.
        Column(modifier = Modifier.padding(Spacing.xxl), content = content)
    }
}

/** A compact KPI tile: colored icon chip, a label, and a large value. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    shadowElevation: Dp = 0.dp,
) {
    MoneyCard(modifier = modifier, shadowElevation = shadowElevation) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(IconSize.md),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.md),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // These tiles carry currency amounts, so the value must never be clipped: a partially shown
        // figure is worse than a missing one because it is silently wrong. At large font scales the
        // amount wraps to a second line and the tile grows, rather than ellipsizing to "S/ 1,2…".
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
    }
}

/** A section title with an optional trailing action (e.g. "Ver todo"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (actionLabel != null && onActionClick != null) {
            // This used to render a primary-colored, tappable-looking label with no `clickable` at
            // all — `onActionClick` was only a visibility guard, so the dashboard's "Ver todo" was
            // inert and TalkBack never announced it as actionable.
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .semantics { role = Role.Button }
                    .heightIn(min = 48.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = Spacing.sm),
            )
        }
    }
}
