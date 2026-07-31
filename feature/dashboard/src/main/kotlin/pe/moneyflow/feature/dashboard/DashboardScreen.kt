package pe.moneyflow.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.ShimmerBox
import pe.moneyflow.core.designsystem.component.StatTile
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.ui.component.TransactionRow

@Composable
fun DashboardScreen(
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A one-tap save is automatic enough to demand a way back: 4 s of deshacer.
    val onShortcut: (QuickShortcut) -> Unit = { shortcut ->
        viewModel.logShortcut(shortcut, uiState.data.currencyCode)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "${shortcut.label} guardado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoShortcut()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        DashboardContent(
            state = uiState,
            onSeeAllTransactions = onSeeAllTransactions,
            onTransactionClick = onTransactionClick,
            onOpenUpcoming = onOpenUpcoming,
            onOpenBudgets = onOpenBudgets,
            onPreviousMonth = viewModel::showPreviousMonth,
            onNextMonth = viewModel::showNextMonth,
            onShortcut = onShortcut,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenBudgets: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShortcut: (QuickShortcut) -> Unit,
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

        // Card order is deliberate: shortcuts (act), payments nudge (act), budgets at risk
        // (decide), then today's numbers. Everything merely informative moved to Análisis.
        if (state.shortcuts.isNotEmpty()) {
            item {
                ShortcutsRow(
                    shortcuts = state.shortcuts,
                    currencyCode = data.currencyCode,
                    onShortcut = onShortcut,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.streak.isNotEmpty()) {
            item { StreakRow(days = state.streak, modifier = Modifier.fillMaxWidth()) }
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
        // Replaced the "Movimientos" count (a number that enables no decision) with what the
        // month still owes — in amber, and deliberately outside the spent total.
        StatTile(
            label = "Por pagar",
            value = Money.format(data.monthPendingMinor, data.currencyCode),
            icon = Icons.Rounded.Schedule,
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            shadowElevation = 0.dp,
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
