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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.util.launchPaymentApp
import pe.moneyflow.core.ui.util.toShortLabel

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
                    uiState.sections.forEach { section ->
                        item(key = "header-${section.bucket}") {
                            SectionHeaderRow(
                                label = section.label,
                                total = Money.format(section.totalMinor, uiState.currencyCode),
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
                                        onMarkPaid = {
                                            settleWithUndo(payment, uiState.suggestedMethodFor(payment))
                                        },
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
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit,
) {
    // Projections have no row to delete, so they get no swipe affordance at all — that is the whole
    // safety story: a gesture can never destroy a schedule from this screen.
    if (payment.isProjected) {
        UpcomingRow(payment = payment, onClick = onClick, onPay = onPay, onMarkPaid = onMarkPaid)
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
        UpcomingRow(
            payment = payment,
            onClick = onClick,
            onPay = onPay,
            onMarkPaid = onMarkPaid,
        )
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

@Composable
private fun UpcomingRow(
    payment: UpcomingPayment,
    onClick: () -> Unit,
    onPay: () -> Unit,
    onMarkPaid: () -> Unit,
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
        modifier = Modifier
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
        Text(
            text = Money.format(tx.amountMinor, tx.currencyCode),
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
        )
        // Pagar opens the pay sheet for every pending row — the sheet itself explains when the
        // method has no app to launch, instead of the affordance silently disappearing. The glyph
        // is Payments, not OpenInNew: a cash row leaves this app for nothing.
        FilledTonalIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onPay()
            },
            modifier = Modifier.padding(start = Spacing.sm),
        ) {
            Icon(Icons.Rounded.Payments, contentDescription = "Pagar")
        }
        // Only real rows can be marked paid — there is no id to settle on a projection, and the
        // deep-link path above is the one place a forecast turns into ledger data.
        if (!isProjected) {
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMarkPaid()
                },
                modifier = Modifier.padding(start = Spacing.xs),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Marcar como pagado",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
