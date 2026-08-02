package pe.moneyflow.feature.dashboard

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
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
import pe.moneyflow.core.domain.model.SpendingPace
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
    // No horizontal contentPadding: the hero band runs edge to edge, so the inset belongs to the
    // items below it rather than to the list.
    val sidePadding = Modifier.padding(horizontal = Spacing.lg)
    val listState = rememberLazyListState()
    val bandCoversStatusBar = listState.firstVisibleItemIndex == 0
    StatusBarIconsOverBand(bandCoversStatusBar = bandCoversStatusBar)
    Box(modifier = modifier.fillMaxWidth()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = 96.dp, // clear the docked FAB / bottom bar
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // The hero band is the top of the screen: it owns the month selector and the streak, and the
        // shell draws no app bar on this destination. "Hola 👋" is long gone — it carried no
        // information, had no name to personalize with, and rendered inconsistently across OEM emoji
        // fonts, all while occupying the most valuable space on the screen.
        item {
            HeroBalanceCard(
                data = data,
                pace = state.pace,
                canGoForward = state.canGoForward,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                streak = if (state.isLoading) emptyList() else state.streak,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.isLoading) {
            item { LoadingSkeleton(modifier = sidePadding) }
            return@LazyColumn
        }

        // Card order is deliberate: shortcuts (act), payments nudge (act), budgets at risk
        // (decide), then today's numbers. Everything merely informative moved to Análisis.
        if (state.shortcuts.isNotEmpty()) {
            item {
                ShortcutsRow(
                    shortcuts = state.shortcuts,
                    currencyCode = data.currencyCode,
                    onShortcut = onShortcut,
                    modifier = sidePadding.fillMaxWidth(),
                )
            }
        } else if (state.isCurrentMonth) {
            // Explains itself instead of rendering nothing. Skipping the onboarding step leaves this
            // row empty until the ledger is 30 days old, which is the documented degradation — but a
            // headline feature that is simply absent, with no trace, is indistinguishable from one
            // that was never built. Past months are excluded: shortcuts are deliberately empty there
            // and log to *today*, so the explanation would be nonsense.
            item { ShortcutsEmptyCard(modifier = sidePadding.fillMaxWidth()) }
        }

        state.upcomingNudge?.let { nudge ->
            item {
                UpcomingNudgeCard(
                    nudge = nudge,
                    currencyCode = data.currencyCode,
                    onClick = onOpenUpcoming,
                    modifier = sidePadding,
                )
            }
        }

        // Budgets, promoted onto the dashboard. Previously the answer to "am I within budget?" — the
        // primary question in an expense app — was three taps away behind "Más".
        if (state.topBudgets.isNotEmpty()) {
            item {
                BudgetSummaryCard(
                    budgets = state.topBudgets,
                    onOpenBudgets = onOpenBudgets,
                    modifier = sidePadding.fillMaxWidth(),
                )
            }
        }

        item { StatRow(data, pace = state.pace, modifier = sidePadding) }

        item {
            SectionHeader(
                title = "Movimientos recientes",
                actionLabel = if (data.recent.isNotEmpty()) "Ver todo" else null,
                onActionClick = if (data.recent.isNotEmpty()) onSeeAllTransactions else null,
                modifier = sidePadding.fillMaxWidth(),
            )
        }

        if (data.recent.isEmpty()) {
            item {
                MoneyCard(modifier = sidePadding.fillMaxWidth(), shadowElevation = 0.dp) {
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
                    modifier = sidePadding.fillMaxWidth(),
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

    // Once the band has scrolled away the list runs edge to edge under the status bar, so rows
    // collide with the clock. This backs the bar with the page colour the moment the band stops
    // covering it — the band keeps the top edge while it is there, and nothing overlaps once it
    // isn't. Drawn over the list rather than padding it, so scrolling stays continuous.
    if (!bandCoversStatusBar) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(MaterialTheme.colorScheme.background),
        )
    }
    }
}

/**
 * Keeps the status-bar icons legible while the brand band sits under them.
 *
 * The band is the first list item, not a pinned header, so it scrolls away — which means a single
 * fixed choice is wrong half the time. Light icons over indigo, then back to the theme's default
 * once the band is gone; leaving them light would make the clock vanish against the page.
 *
 * `firstVisibleItemIndex == 0` is the exact test: while item 0 is the first visible item it still
 * occupies the top of the viewport, however far it has been scrolled.
 */
@Composable
private fun StatusBarIconsOverBand(bandCoversStatusBar: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    DisposableEffect(bandCoversStatusBar, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = if (bandCoversStatusBar) false else !isDarkTheme
        // Restored on the way out, so leaving Inicio doesn't strand another screen with light icons.
        onDispose { controller?.isAppearanceLightStatusBars = !isDarkTheme }
    }
}

@Composable
private fun UpcomingNudgeCard(
    nudge: UpcomingNudge,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    MoneyCard(modifier = modifier.fillMaxWidth(), shadowElevation = 0.dp) {
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
private fun StatRow(data: DashboardData, pace: SpendingPace?, modifier: Modifier = Modifier) {
    // What the month still owes. Takes the *committed* figure rather than `monthPendingMinor`,
    // which counts only pending rows that exist in the ledger and so ignores occurrences still
    // projected from recurring templates. That made the tile read "S/ 0.00" on a month where the
    // hero said "Ya comprometido: S/ 10,279" and Próximos listed the same S/ 10,279 — three
    // numbers about one month, with this one disagreeing. Past months have no pace, and nothing
    // is "still owed" in a month already gone, so they fall back to the materialized total.
    val pendingMinor = pace?.committedRemainingMinor ?: data.monthPendingMinor
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
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
            value = Money.format(pendingMinor, data.currencyCode),
            icon = Icons.Rounded.Schedule,
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
            shadowElevation = 0.dp,
        )
    }
}

@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(140.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
    }
}
