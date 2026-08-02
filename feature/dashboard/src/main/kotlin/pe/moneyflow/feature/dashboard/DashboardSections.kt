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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.MoneyProgressBar
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.pressScale
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.illustration.Illustration
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
import java.time.format.TextStyle
import java.util.Locale
import pe.moneyflow.core.ui.util.money

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
    // Defaults suit a normal surface; the hero band passes its own on-brand pair.
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    mutedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = contentColor,
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
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Mes siguiente",
                // Explicit rather than IconButton's disabled default, which is alpha over the content
                // color; the muted token carries "not available" without going translucent.
                tint = if (canGoForward) contentColor else mutedColor,
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
 * "De un toque": the block that takes logging a habitual expense from five taps to one.
 *
 * Each card carries its description and its usual amount, so the tap is a decision the user already
 * made — nothing to confirm. The save is automatic, so it always offers deshacer, and it gets the
 * FAB's haptic to feel like the same class of action.
 *
 * A two-column grid rather than a horizontal chip strip: the strip put anything past the second
 * shortcut off-screen, so the one-tap path was only one tap for the two the heuristic happened to
 * rank first. A grid shows all four at once, and the taller card fits the amount on its own line
 * where it can be read at a glance instead of run together with the label.
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
        // Chunked into rows of two rather than a LazyVerticalGrid: this sits inside a LazyColumn,
        // where a nested lazy grid in the same direction cannot measure.
        shortcuts.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                pair.forEach { shortcut ->
                    val interaction = remember { MutableInteractionSource() }
                    MoneyCard(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp)
                            .pressScale(interaction)
                            .clickable(interactionSource = interaction, indication = null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShortcut(shortcut)
                            },
                        shadowElevation = 0.dp,
                        contentPadding = PaddingValues(Spacing.md),
                    ) {
                        Text(
                            text = shortcut.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = money(shortcut.amountMinor, currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                // Keeps a lone trailing shortcut half-width instead of stretching it across the row.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * The whole of Inicio on a ledger with nothing in it: one ask, with the button it names.
 *
 * Replaces the movements empty state *and* the shortcuts card in this state. The FAB can do the
 * same thing, but it is the smallest element on screen and carries no words — a first-time user is
 * being asked to trust an icon. This says what to do and why it is worth doing.
 */
@Composable
fun FirstRunCard(onAddExpense: () -> Unit, modifier: Modifier = Modifier) {
    MoneyCard(modifier = modifier, shadowElevation = 0.dp) {
        EmptyState(
            illustration = Illustration.NoTransactions,
            title = "Empieza por un gasto de hoy",
            subtitle = "Un almuerzo, un pasaje, lo que tengas a mano. Toma unos segundos.",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        Button(
            onClick = onAddExpense,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(IconSize.chip),
            )
            Text(
                text = "Registrar un gasto",
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

/**
 * The 30-day shortcuts rule as one line, not a card.
 *
 * On a first run [ShortcutsEmptyCard] is a full card explaining an absence, stacked under another
 * card explaining the same absence. Here it is context — and, unlike before, a way in: the picker
 * that onboarding offered once is now a destination, so skipping that step is recoverable.
 */
@Composable
fun ShortcutsPendingLine(onOpenShortcuts: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpenShortcuts, role = Role.Button)
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.sm),
        )
        Text(
            text = "Tus atajos de un toque se activan a los 30 días de historial.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The "no shortcuts yet" counterpart to [ShortcutsRow].
 *
 * States the actual rule rather than a placeholder, because the rule is the whole answer to "why is
 * this empty?" — `GetFrequentShortcutsUseCase` returns nothing until the oldest movement in the
 * ledger is 30 days old, so a two-week-old install has no shortcuts no matter how repetitive its
 * spending is.
 *
 * The wait is no longer the only option: picking them explicitly is a destination now, so a user
 * who skipped onboarding's step 4 does not have to serve the 30 days to get the feature back.
 */
@Composable
fun ShortcutsEmptyCard(onOpenShortcuts: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "De un toque",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
        MoneyCard(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp,
            contentPadding = PaddingValues(Spacing.lg),
        ) {
            Text(
                text = "Tus gastos más repetidos aparecerán aquí para registrarlos en un toque. " +
                    "Se activan cuando tu historial cumpla 30 días.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onOpenShortcuts,
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = Spacing.sm),
            ) { Text("Elegirlos ahora") }
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
fun StreakRow(
    days: List<StreakDay>,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    mutedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val loggedCount = days.count { it.logged }
    val over = MaterialTheme.colorScheme.tertiaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Racha: $loggedCount de ${days.size} días registrados"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Each dot is captioned with its weekday initial. Seven bare dots said "seven of something"
        // and nothing more — you could not tell which day you had missed, which is the only thing a
        // streak is for.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            days.forEach { day ->
                val filled = when {
                    !day.logged -> Color.Transparent
                    day.withinAllowance == false -> over
                    else -> contentColor
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day.date.dayOfWeek
                            .getDisplayName(TextStyle.NARROW, Locale("es"))
                            .uppercase(Locale("es")),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor,
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(filled)
                            .border(1.dp, if (day.logged) filled else mutedColor, CircleShape),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Spacing.xxs))
            Text(
                text = "$loggedCount/${days.size}",
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
        }
    }
}
