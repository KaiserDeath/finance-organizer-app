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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import pe.moneyflow.core.ui.safearea.safeArea
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.ui.component.TransactionRow
import pe.moneyflow.core.ui.paysheet.PaySheet
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.util.launchPaymentApp
import pe.moneyflow.core.ui.util.money
import pe.moneyflow.core.ui.util.toShortLabel

@Composable
fun DashboardScreen(
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenBudgets: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenShortcuts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var paySheetFor by remember { mutableStateOf<UpcomingPayment?>(null) }
    var awaitingReturn by remember { mutableStateOf<Pair<UpcomingPayment, PaymentMethod>?>(null) }
    var wasPaused by remember { mutableStateOf(false) }

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

    val settleWithUndo: (UpcomingPayment, PaymentMethod?) -> Unit = { payment, method ->
        viewModel.settle(payment, method?.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = if (method != null) "Pagado con ${method.name}" else "Pagado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoSettle()
        }
    }

    LifecycleResumeEffect(Unit) {
        val pending = awaitingReturn
        if (wasPaused && pending != null) {
            awaitingReturn = null
            settleWithUndo(pending.first, pending.second)
        }
        wasPaused = false
        onPauseOrDispose { wasPaused = true }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.safeArea("snackbar_dashboard")) },
    ) { innerPadding ->
        DashboardContent(
            state = uiState,
            onSeeAllTransactions = onSeeAllTransactions,
            onTransactionClick = onTransactionClick,
            onOpenUpcoming = onOpenUpcoming,
            onPayUpcoming = { paySheetFor = it },
            onOpenBudgets = onOpenBudgets,
            onAddExpense = onAddExpense,
            onOpenShortcuts = onOpenShortcuts,
            onPreviousMonth = viewModel::showPreviousMonth,
            onNextMonth = viewModel::showNextMonth,
            onShortcut = onShortcut,
            onToggleAmountsHidden = viewModel::toggleAmountsHidden,
            modifier = Modifier.padding(innerPadding),
        )
    }

    paySheetFor?.let { payment ->
        PaySheet(
            payment = payment,
            methods = uiState.methodsById.values.toList(),
            suggestedMethodId = uiState.suggestedMethodFor(payment)?.id,
            onLaunchApp = { method ->
                paySheetFor = null
                val launched = launchPaymentApp(
                    context = context,
                    packageName = method.deepLinkPackage,
                    appName = method.name,
                    playStoreId = method.playStoreId,
                    webBankingUrl = FinancePresets.webUrlFor(method.deepLinkPackage),
                )
                if (launched) awaitingReturn = payment to method
            },
            onSettle = { method ->
                paySheetFor = null
                settleWithUndo(payment, method)
            },
            onDismiss = { paySheetFor = null },
        )
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
    onPayUpcoming: (UpcomingPayment) -> Unit,
    onOpenBudgets: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenShortcuts: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShortcut: (QuickShortcut) -> Unit,
    onToggleAmountsHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = state.data
    // No horizontal contentPadding: the hero band runs edge to edge, so the inset belongs to the
    // items below it rather than to the list.
    val sidePadding = Modifier.padding(horizontal = Spacing.lg)
    val listState = rememberLazyListState()
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
                onOpenBudgets = onOpenBudgets,
                isFirstRun = state.isFirstRun,
                onToggleAmountsHidden = onToggleAmountsHidden,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.isLoading) {
            item { LoadingSkeleton(modifier = sidePadding) }
            return@LazyColumn
        }

        // A ledger with nothing in it has one useful screen: the one that asks for the first entry.
        // Everything below is a variation on "empty", and three of them stacked read as a broken
        // app rather than a new one.
        if (state.isFirstRun) {
            item {
                FirstRunCard(
                    onAddExpense = onAddExpense,
                    modifier = sidePadding.fillMaxWidth(),
                )
            }
            item {
                ShortcutsPendingLine(
                    onOpenShortcuts = onOpenShortcuts,
                    modifier = sidePadding.fillMaxWidth(),
                )
            }
            return@LazyColumn
        }

        // Card order is deliberate: shortcuts (act), payments nudge (act), budgets at risk
        // (decide), then today's numbers. Everything merely informative moved to Análisis.
        if (state.shortcuts.isNotEmpty()) {
            item {
                ShortcutsRow(
                    shortcuts = state.shortcuts,
                    currencyCode = data.currencyCode,
                    categoriesById = data.categoriesById,
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
            item {
                ShortcutsEmptyCard(
                    onOpenShortcuts = onOpenShortcuts,
                    modifier = sidePadding.fillMaxWidth(),
                )
            }
        }

        // Payments are the next action after shortcuts: a pending or overdue charge should not be
        // buried underneath the history list when the user opens Inicio to see what needs doing.
        state.upcomingNudge?.let { nudge ->
            item {
                UpcomingNudgeCard(
                    nudge = nudge,
                    currencyCode = data.currencyCode,
                    onClick = onOpenUpcoming,
                    onPay = onPayUpcoming,
                    modifier = sidePadding,
                )
            }
        }

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
                Column(modifier = sidePadding.fillMaxWidth()) {
                    data.recent.take(4).forEachIndexed { index, tx ->
                        TransactionRow(
                            transaction = tx,
                            category = tx.categoryId?.let { data.categoriesById[it] },
                            onClick = { onTransactionClick(tx.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (index < data.recent.take(4).lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
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

    }

    // Once the band has scrolled away the list runs edge to edge under the status bar, so rows
    // collide with the clock. This backs the bar with the page colour the moment the band stops
    // covering it — the band keeps the top edge while it is there, and nothing overlaps once it
    // isn't. Drawn over the list rather than padding it, so scrolling stays continuous.
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
    onPay: (UpcomingPayment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasOverdue = nudge.overdueCount > 0
    val accent =
        if (hasOverdue) MaterialTheme.moneyColors.negative
        else MaterialTheme.colorScheme.tertiary
    // Urgency has to survive without color: a warning carried only by tint fails for anyone who
    // can't see it, and it dropped the actual count. The subtitle now says it in words too.
    val subtitle = buildString {
        append("Total ")
        append(money(nudge.totalAmountMinor, currencyCode))
        if (hasOverdue) {
            append(" · ")
            append(nudge.overdueCount)
            append(if (nudge.overdueCount == 1) " vencido" else " vencidos")
        }
    }
    MoneyCard(modifier = modifier.fillMaxWidth(), shadowElevation = 0.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
                        text = "Por pagar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasOverdue) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Ver próximos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Pendiente de pago — no incluido en el gasto del mes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                nudge.payments.forEach { payment ->
                    val dueDate = payment.dueDate
                    val payeeLabel = payment.transaction.description ?: payment.transaction.title
                    val amountLabel = money(payment.transaction.amountMinor, currencyCode)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = payeeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = when {
                                    dueDate == null -> "Sin fecha"
                                    payment.isProjected -> "Programado · ${dueDate.toShortLabel()}"
                                    else -> dueDate.toShortLabel()
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = amountLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = Spacing.sm),
                        )
                        TextButton(
                            onClick = { onPay(payment) },
                            modifier = Modifier
                                .safeArea("action_pay_${payment.transaction.id}")
                                .semantics {
                                    contentDescription = "Pagar $payeeLabel, $amountLabel"
                                },
                        ) { Text("Pagar") }
                    }
                }
                if (nudge.hiddenCount > 0) {
                    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("y ${nudge.hiddenCount} más")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(data: DashboardData, pace: SpendingPace?, modifier: Modifier = Modifier) {
    // What the month still owes. Takes the *committed* figure rather than `monthPendingMinor`,
    // which counts only pending rows that exist in the ledger and so ignores occurrences still
    // projected from recurring templates. That made the tile read "S/ 0.00" on a month where the
    // band and Próximos both said S/ 10,279 — three numbers about one month, with this one
    // disagreeing. The band no longer prints it at all (this tile is the one place it lives now).
    // Past months have no pace, and nothing is "still owed" in a month already gone, so they fall
    // back to the materialized total.
    val pendingMinor = pace?.committedRemainingMinor ?: data.monthPendingMinor
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        StatTile(
            label = "Hoy",
            value = money(data.todaySpentMinor, data.currencyCode),
            icon = Icons.Rounded.CalendarToday,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            shadowElevation = 0.dp,
        )
        // Replaced the "Movimientos" count (a number that enables no decision) with what the
        // month still owes — in amber, and deliberately outside the spent total.
        StatTile(
            label = "Por pagar",
            value = money(pendingMinor, data.currencyCode),
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
