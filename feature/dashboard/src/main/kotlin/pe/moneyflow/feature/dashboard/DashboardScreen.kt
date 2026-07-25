package pe.moneyflow.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import pe.moneyflow.core.designsystem.theme.PositiveGreen
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.component.TransactionRow
import pe.moneyflow.core.ui.util.toMonthTitle
import java.time.LocalDate

@Composable
fun DashboardScreen(
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = uiState,
        onSeeAllTransactions = onSeeAllTransactions,
        onTransactionClick = onTransactionClick,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
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
        item { DashboardHeader() }

        if (state.isLoading) {
            item { LoadingSkeleton() }
            return@LazyColumn
        }

        item { HeroBalanceCard(data) }
        item { StatRow(data) }

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
                MoneyCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
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
private fun DashboardHeader() {
    Column {
        Text(
            text = "Hola 👋",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = LocalDate.now().toMonthTitle(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HeroBalanceCard(data: DashboardData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Gastado este mes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = Money.format(data.monthSpentMinor, data.currencyCode),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            HeroStat(
                label = "Ingresos",
                value = Money.format(data.monthIncomeMinor, data.currencyCode),
                valueColor = PositiveGreen,
            )
            HeroStat(
                label = "Balance",
                value = Money.format(data.monthIncomeMinor - data.monthSpentMinor, data.currencyCode),
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
        )
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
        )
        StatTile(
            label = "Movimientos",
            value = data.recent.size.toString(),
            icon = Icons.Rounded.TrendingUp,
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CategoryBreakdownCard(data: DashboardData) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
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
            DonutChart(
                slices = slices,
                diameter = 132.dp,
                centerContent = {
                    Text(
                        text = "${data.categoryBreakdown.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
