package pe.moneyflow.feature.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.MoneyProgressBar
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.pressScale
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.model.StreakDay
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.util.toMonthTitle
import java.time.YearMonth

/**
 * Month navigation for the dashboard.
 *
 * The month used to be static text rendered at heading weight, sitting exactly where a selector
 * belongs — so users would tap it and nothing happened, and there was no way to view any month but the
 * current one. "How did last month go?" is a top-three question for an expense app and had no answer
 * anywhere in the UI.
 *
 * Forward navigation stops at the current month: there is nothing to show in the future, and a live
 * arrow that does nothing is the same mistake as the un-tappable title.
 */
@Composable
fun MonthSelector(
    month: YearMonth,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Mes anterior",
            )
        }
        // Slides in the direction of travel, so the change reads as moving along a timeline.
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val forward = targetState > initialState
                val width = if (forward) 1 else -1
                (
                    slideInHorizontally(Motion.offset()) { w -> width * w / 3 } +
                        fadeIn(Motion.effectsDefault())
                    ) togetherWith (
                    slideOutHorizontally(Motion.offset()) { w -> -width * w / 3 } +
                        fadeOut(Motion.effectsDefault())
                    ) using SizeTransform(clip = false)
            },
            label = "month-label",
        ) { shown ->
            Text(
                text = shown.toMonthTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Mes siguiente",
            )
        }
    }
}

/**
 * Top budgets, on the dashboard.
 *
 * Shows the ones closest to their limit rather than the largest, because a category at 95% is the one
 * worth knowing about. Reuses the same threshold vocabulary as the Budgets screen — red for breached,
 * amber past 80% — so the colours mean the same thing everywhere.
 */
@Composable
fun BudgetSummaryCard(
    budgets: List<BudgetProgress>,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoneyCard(modifier = modifier) {
        SectionHeader(
            title = "Presupuestos",
            actionLabel = "Ver todo",
            onActionClick = onOpenBudgets,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            budgets.forEach { progress -> BudgetMiniRow(progress) }
        }
    }
}

@Composable
private fun BudgetMiniRow(progress: BudgetProgress) {
    val barColor = when {
        progress.isOverBudget -> MaterialTheme.moneyColors.negative
        progress.isNearLimit -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val accent = colorFromHex(progress.category?.colorHex, MaterialTheme.colorScheme.primary)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(
                icon = iconForKey(progress.category?.iconKey ?: "savings"),
                accent = accent,
                size = 28.dp,
            )
            Text(
                text = progress.budget.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.sm),
            )
            Text(
                text = "${progress.percentUsed}%",
                style = MaterialTheme.typography.labelLarge,
                color = barColor,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        MoneyProgressBar(fraction = progress.fraction, color = barColor, height = 6.dp)
    }
}

/**
 * "De un toque": the row that takes logging a habitual expense from five taps to one.
 *
 * Each chip carries its description and its usual amount, so the tap is a decision the user
 * already made — nothing to confirm. The save is automatic, so it always offers deshacer, and it
 * gets the FAB's haptic to feel like the same class of action.
 */
@Composable
fun ShortcutsRow(
    shortcuts: List<QuickShortcut>,
    currencyCode: String,
    onShortcut: (QuickShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "De un toque",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(items = shortcuts, key = { it.label + it.amountMinor }) { shortcut ->
                val interaction = remember { MutableInteractionSource() }
                AssistChip(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShortcut(shortcut)
                    },
                    interactionSource = interaction,
                    label = {
                        Text("${shortcut.label} · ${Money.format(shortcut.amountMinor, currencyCode)}")
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .pressScale(interaction),
                )
            }
        }
    }
}

/**
 * Seven dots, one per day, against that day's variable allowance.
 *
 * The only non-actionable element left on the dashboard, kept on purpose: it doesn't inform a
 * decision, it sustains the habit of logging. If sessions show it reads as decoration, it goes.
 */
@Composable
fun StreakRow(days: List<StreakDay>, modifier: Modifier = Modifier) {
    val loggedCount = days.count { it.logged }
    val outline = MaterialTheme.colorScheme.outlineVariant
    val within = MaterialTheme.colorScheme.primary
    val over = MaterialTheme.colorScheme.tertiary

    MoneyCard(modifier = modifier, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Racha: $loggedCount de ${days.size} días registrados"
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Racha",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                days.forEach { day ->
                    val color = when {
                        !day.logged -> Color.Transparent
                        day.withinAllowance == false -> over
                        else -> within
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, if (day.logged) color else outline, CircleShape),
                    )
                }
            }
        }
    }
}
