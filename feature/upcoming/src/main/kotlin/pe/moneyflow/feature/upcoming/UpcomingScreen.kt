package pe.moneyflow.feature.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.illustration.Illustration
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SkeletonBlocks
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.paysheet.PayBatchSheet
import pe.moneyflow.core.ui.paysheet.PaySheet
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.util.launchPaymentApp
import pe.moneyflow.core.ui.util.toShortLabel
import pe.moneyflow.core.ui.util.money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    onPaymentClick: (String) -> Unit,
    onOpenRecurring: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The pay sheet target (null = closed) and the payment we launched an external app for,
    // waiting for the user to come back so we can settle it.
    var paySheetFor by remember { mutableStateOf<UpcomingPayment?>(null) }
    var batchSheetOpen by remember { mutableStateOf(false) }
    var awaitingReturn by remember { mutableStateOf<Pair<UpcomingPayment, PaymentMethod>?>(null) }
    var wasPaused by remember { mutableStateOf(false) }

    // Settling is a real mutation (status → PAID, stamps today, records the method): confirm it
    // and offer a way back.
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

    // Coming back from the bank/wallet app settles the charge. The paused flag guards against
    // the first resume (entering the screen) counting as a return.
    LifecycleResumeEffect(Unit) {
        val pending = awaitingReturn
        if (wasPaused && pending != null) {
            awaitingReturn = null
            settleWithUndo(pending.first, pending.second)
        }
        wasPaused = false
        onPauseOrDispose { wasPaused = true }
    }

    val onDelete: (UpcomingPayment) -> Unit = { payment ->
        viewModel.delete(payment.transaction)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Movimiento eliminado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // A stacked destination gets no bar from the shell, so it owns one — which is also where
        // the link to the schedule belongs. Análisis reaches its report the same way.
        topBar = {
            TopAppBar(
                title = { Text("Próximos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRecurring) {
                        Icon(Icons.Rounded.EventRepeat, contentDescription = "Pagos recurrentes")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                SkeletonBlocks(
                    modifier = Modifier.padding(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = Spacing.md,
                    ),
                    count = 3,
                    blockHeight = 160.dp,
                )
            } else if (uiState.isEmpty) {
                EmptyState(
                    illustration = Illustration.NothingDue,
                    title = "Nada por pagar",
                    subtitle = "Registra un gasto como \"Pendiente\" con su fecha para verlo aquí.",
                    modifier = Modifier.fillMaxSize().padding(Spacing.xl),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = Spacing.md,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    // Offered only for a real pile-up. One overdue bill is a row with a button;
                    // this exists because three of them meant three round-trips through the sheet,
                    // each with its own undo window expiring in the order it opened.
                    val overdue = uiState.settleableOverdue
                    if (overdue.size > 1) {
                        item(key = "batch-overdue") {
                            BatchSettleCard(
                                count = overdue.size,
                                onPayAll = { batchSheetOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    uiState.sections.forEach { section ->
                        item(key = "header-${section.bucket}") {
                            SectionHeaderRow(
                                label = section.label,
                                total = money(section.totalMinor, uiState.currencyCode),
                                isOverdue = section.items.any { it.isOverdue },
                            )
                        }
                        item(key = "card-${section.bucket}") {
                            MoneyCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = Spacing.xs),
                            ) {
                                section.items.forEach { payment ->
                                    SwipeableUpcomingRow(
                                        payment = payment,
                                        onClick = {
                                            // A projection has no detail screen; open its template.
                                            if (payment.isProjected) {
                                                onOpenRecurring()
                                            } else {
                                                onPaymentClick(payment.transaction.id)
                                            }
                                        },
                                        onPay = { paySheetFor = payment },
                                        onDelete = { onDelete(payment) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
                if (launched) {
                    awaitingReturn = payment to method
                } else {
                    scope.launch { snackbarHostState.showSnackbar("No se pudo abrir ${method.name}") }
                }
            },
            onSettle = { method ->
                paySheetFor = null
                settleWithUndo(payment, method)
            },
            onDismiss = { paySheetFor = null },
        )
    }

    if (batchSheetOpen) {
        val overdue = uiState.settleableOverdue
        PayBatchSheet(
            payments = overdue,
            methods = uiState.methodsById.values.toList(),
            // The batch has no single history to draw on, so it opens on the user's default.
            suggestedMethodId = uiState.methodsById.values.firstOrNull { it.isDefault }?.id,
            currencyCode = uiState.currencyCode,
            onSettleAll = { method ->
                batchSheetOpen = false
                viewModel.settleAll(overdue, method?.id)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "${overdue.size} pagos registrados",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoSettle()
                }
            },
            onDismiss = { batchSheetOpen = false },
        )
    }
}

/** The way out of a pile-up: one sheet, one method, one undo. */
@Composable
private fun BatchSettleCard(count: Int, onPayAll: () -> Unit, modifier: Modifier = Modifier) {
    MoneyCard(modifier = modifier, contentPadding = PaddingValues(Spacing.lg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(IconSize.sm),
            )
            Text(
                text = "$count vencidos, un mismo método",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onPayAll,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Pagar todos") }
        }
    }
}

@Composable
private fun SectionHeaderRow(label: String, total: String, isOverdue: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (isOverdue) MaterialTheme.moneyColors.negative else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = total,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableUpcomingRow(
    payment: UpcomingPayment,
    onClick: () -> Unit,
    onPay: () -> Unit,
    onDelete: () -> Unit,
) {
    // Projections have no row to delete, so they get no swipe affordance at all — that is the whole
    // safety story: a gesture can never destroy a schedule from this screen.
    if (payment.isProjected) {
        UpcomingRow(payment = payment, onClick = onClick, onPay = onPay)
        return
    }

    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() },
    ) {
        UpcomingRow(payment = payment, onClick = onClick, onPay = onPay)
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = "Eliminar",
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

// internal rather than private so the instrumented font-scale test can compose the row directly;
// its width behaviour is the thing under test and a wrapper would not reproduce it.
@Composable
internal fun UpcomingRow(
    payment: UpcomingPayment,
    onClick: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tx = payment.transaction
    val isOverdue = payment.isOverdue
    val isProjected = payment.isProjected
    val haptics = LocalHapticFeedback.current
    val baseAccent = colorFromHex(payment.category?.colorHex, MaterialTheme.colorScheme.primary)
    val dueLabel = payment.dueDate?.toShortLabel() ?: "Sin fecha"

    // A forecast reads lighter than a real obligation: same layout, lower contrast.
    val accent = if (isProjected) baseAccent.copy(alpha = 0.45f) else baseAccent
    val titleColor = if (isProjected) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Opaque so the delete background stays hidden until the row is actually swiped.
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(icon = iconForKey(payment.category?.iconKey ?: "category"), accent = accent)
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            Text(
                text = tx.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    isOverdue -> "Venció el $dueLabel"
                    isProjected -> "Programado · $dueLabel"
                    else -> dueLabel
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOverdue) MaterialTheme.moneyColors.negative else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Amount and action stack rather than sitting inline: inline, the 16sp title truncated to
        // roughly 100dp ("Internet — Mov…"), and the label is what the action was missing.
        Column(
            modifier = Modifier.padding(start = Spacing.sm),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = money(tx.amountMinor, tx.currencyCode),
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            // One primary control per row. Settling used to live here too, as a second icon button
            // doing what the sheet's "Ya pagué por fuera" already does with a label, a recorded
            // method and undo — two controls, one behaviour, no way to tell them apart.
            //
            // The pill draws at 32dp but is hit-tested at 48dp.
            //
            // The 48dp lives on the clickable node itself, not on a wrapper and not on
            // minimumInteractiveComponentSize. An M3 Button keeps its clickable in an internal
            // Surface that outer modifiers cannot reach (target stayed 34dp), and
            // minimumInteractiveComponentSize is gated behind a CompositionLocal this row does not
            // set (target stayed 32dp). Both were caught by the instrumented test at 200% font
            // scale; this is the version that measures 48dp.
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPay()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    // Payments, not OpenInNew: a cash row leaves this app for nothing.
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(IconSize.chip),
                    )
                    Text(
                        text = "Pagar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
