package pe.moneyflow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.ui.util.redactAmounts

/**
 * The one place an [Insight] is rendered.
 *
 * Previously the dashboard and the insights screen each had their own copy, and they *disagreed on
 * the warning color* — the dashboard used amber (`tertiary`) while the insights list used red. The
 * same insight changed color depending on which screen you were looking at.
 *
 * Amber wins: an insight is advisory. It shares the caution vocabulary already established by
 * budget thresholds, where amber means "approaching the limit" and red is reserved for a limit
 * already breached. Red on a suggestion over-alarms.
 *
 * Pass [onClick] to make the card navigable — it then shows a trailing chevron. Omit it for a
 * terminal list item.
 */
@Composable
fun InsightCard(
    insight: Insight,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    maxMessageLines: Int = Int.MAX_VALUE,
    shadowElevation: Dp = 0.dp,
) {
    val accent = insight.severity.accent()
    MoneyCard(modifier = modifier, shadowElevation = shadowElevation) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = if (onClick != null) Alignment.CenterVertically else Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = insight.kind.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(IconSize.lg),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // Insight text is assembled in the domain with amounts already
                    // formatted into it, so the mask has to be applied to the sentence.
                    text = redactAmounts(insight.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = maxMessageLines,
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Single source of truth for insight tone. See the note on [InsightCard] about amber vs red. */
@Composable
fun InsightSeverity.accent(): Color = when (this) {
    InsightSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
    InsightSeverity.POSITIVE -> MaterialTheme.moneyColors.positive
    InsightSeverity.INFO -> MaterialTheme.colorScheme.primary
}

private fun InsightKind.icon(): ImageVector = when (this) {
    InsightKind.GETTING_STARTED -> Icons.Rounded.Lightbulb
    InsightKind.CASHFLOW -> Icons.AutoMirrored.Rounded.TrendingUp
    InsightKind.SPENDING_SPIKE -> Icons.Rounded.Warning
    InsightKind.TOP_CATEGORY -> Icons.Rounded.Category
    InsightKind.UPCOMING_BILLS -> Icons.Rounded.CalendarMonth
    InsightKind.OVERDUE_BILLS -> Icons.Rounded.Savings
}
