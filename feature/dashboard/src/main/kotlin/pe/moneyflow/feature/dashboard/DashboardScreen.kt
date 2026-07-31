package pe.moneyflow.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.DonutChart
import pe.moneyflow.core.designsystem.component.DonutSlice
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.GlassCard
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.ShimmerBox
import pe.moneyflow.core.designsystem.component.StatTile
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.CategoryPalette
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.ui.component.AnimatedAmount
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.component.InsightCard
import pe.moneyflow.core.ui.component.TransactionRow
import pe.moneyflow.core.ui.util.toMonthTitle
import java.time.LocalDate

@Composable
fun DashboardScreen(
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = uiState,
        onSeeAllTransactions = onSeeAllTransactions,
        onTransactionClick = onTransactionClick,
        onOpenUpcoming = onOpenUpcoming,
        onOpenInsights = onOpenInsights,
        onOpenBudgets = onOpenBudgets,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenBudgets: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = state.data
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = 96.dp, // clear the docked FAB / bottom bar
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // The month title moved into the shell's collapsing app bar, and "Hola 👋" is gone with it —
        // it carried no information, had no name to personalize with, and rendered inconsistently
        // across OEM emoji fonts, all while occupying the most valuable space on the screen.
        item {
            MonthSelector(
                month = data.month,
                canGoForward = state.canGoForward,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.isLoading) {
            item { LoadingSkeleton() }
            return@LazyColumn
        }

        item {
            HeroBalanceCard(
                data = data,
                pace = state.pace,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.upcomingNudge?.let { nudge ->
            item { UpcomingNudgeCard(nudge = nudge, currencyCode = data.currencyCode, onClick = onOpenUpcoming) }
        }

        // Budgets, promoted onto the dashboard. Previously the answer to "am I within budget?" — the
        // primary question in an expense app — was three taps away behind "Más".
        if (state.topBudgets.isNotEmpty()) {
            item {
                BudgetSummaryCard(
                    budgets = state.topBudgets,
                    onOpenBudgets = onOpenBudgets,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item { StatRow(data) }

        state.topInsight?.let { insight ->
            item {
                InsightCard(
                    insight = insight,
                    onClick = onOpenInsights,
                    maxMessageLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (data.categoryBreakdown.isNotEmpty()) {
            item { CategoryBreakdownCard(data) }
        }

        item {
            SectionHeader(
                title = "Movimientos recientes",
                actionLabel = if (data.recent.isNotEmpty()) "Ver todo" else null,
                onActionClick = if (data.recent.isNotEmpty()) onSeeAllTransactions else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (data.recent.isEmpty()) {
            item {
                MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "Aún no hay gastos",
                        subtitle = "Toca el botón + para registrar tu primer gasto.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            item {
                MoneyCard(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 0.dp,
                    contentPadding = PaddingValues(vertical = Spacing.xs),
                ) {
                    data.recent.forEach { tx ->
                        TransactionRow(
                            transaction = tx,
                            category = tx.categoryId?.let { data.categoriesById[it] },
                            onClick = { onTransactionClick(tx.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingNudgeCard(
    nudge: UpcomingNudge,
    currencyCode: String,
    onClick: () -> Unit,
) {
    val hasOverdue = nudge.overdueCount > 0
    val accent =
        if (hasOverdue) MaterialTheme.moneyColors.negative
        else MaterialTheme.colorScheme.tertiary
    val title = when {
        hasOverdue && nudge.dueSoonCount > 0 ->
            "${nudge.overdueCount} vencido(s) y ${nudge.dueSoonCount} por vencer"
        hasOverdue -> "${nudge.overdueCount} pago(s) vencido(s)"
        else -> "${nudge.dueSoonCount} pago(s) por vencer"
    }

    MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasOverdue) Icons.Rounded.Warning else Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Total ${Money.format(nudge.totalAmountMinor, currencyCode)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Ver próximos",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatRow(data: DashboardData) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        StatTile(
            label = "Hoy",
            value = Money.format(data.todaySpentMinor, data.currencyCode),
            icon = Icons.Rounded.CalendarToday,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            shadowElevation = 0.dp,
        )
        StatTile(
            label = "Movimientos",
            value = data.monthTransactionCount.toString(),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
            shadowElevation = 0.dp,
        )
    }
}

@Composable
private fun CategoryBreakdownCard(data: DashboardData) {
    MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
        SectionHeader(title = "Por categoría", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val slices = data.categoryBreakdown.mapIndexed { index, spend ->
                DonutSlice(
                    fraction = spend.fraction,
                    color = colorFromHex(
                        spend.category.colorHex,
                        CategoryPalette[index % CategoryPalette.size],
                    ),
                )
            }
            val topSpend = data.categoryBreakdown.first()
            // Summed from the slices rather than taken from monthSpentMinor, so the center total is
            // always consistent with the ring actually drawn.
            val breakdownTotalMinor = data.categoryBreakdown.sumOf { it.amountMinor }
            DonutChart(
                slices = slices,
                diameter = 132.dp,
                contentDescription = "Gasto por categoría, total " +
                    "${Money.format(breakdownTotalMinor, data.currencyCode)}. " +
                    "Mayor: ${topSpend.category.name}, ${(topSpend.fraction * 100).toInt()}%.",
                centerContent = {
                    // The center is the most privileged spot in the chart, so it carries the total
                    // spend. It previously showed the *count* of categories, which is close to the
                    // least useful fact available here.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Money.format(breakdownTotalMinor, data.currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (data.categoryBreakdown.size == 1) "1 categoría"
                            else "${data.categoryBreakdown.size} categorías",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
            )
            Spacer(Modifier.size(Spacing.xl))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                data.categoryBreakdown.take(4).forEachIndexed { index, spend ->
                    CategoryLegendRow(
                        name = spend.category.name,
                        amount = Money.format(spend.amountMinor, data.currencyCode),
                        percent = spend.fraction,
                        color = colorFromHex(
                            spend.category.colorHex,
                            CategoryPalette[index % CategoryPalette.size],
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendRow(name: String, amount: String, percent: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(
            text = "${(percent * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(140.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
    }
}
