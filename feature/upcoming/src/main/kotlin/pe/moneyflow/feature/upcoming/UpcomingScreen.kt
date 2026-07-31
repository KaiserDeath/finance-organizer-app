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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.util.launchPaymentApp
import pe.moneyflow.core.ui.util.toShortLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    onPaymentClick: (String) -> Unit,
    onOpenRecurring: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Opens the method's bank/wallet app (or its web/Play Store fallback), then offers to settle
    // the charge on return — the same "close the loop" the detail sheet uses.
    val onPay: (UpcomingPayment) -> Unit = { payment ->
        val method = uiState.methodFor(payment)
        if (method != null) {
            val launched = launchPaymentApp(
                context = context,
                packageName = method.deepLinkPackage,
                appName = method.name,
                playStoreId = method.playStoreId,
                webBankingUrl = FinancePresets.webUrlFor(method.deepLinkPackage),
            )
            if (launched) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "¿Ya pagaste con ${method.name}?",
                        actionLabel = "Marcar pagado",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // A projected occurrence has no row yet, so settling it creates one.
                        if (payment.isProjected) {
                            viewModel.payProjected(payment)
                        } else {
                            viewModel.markPaid(payment.transaction.id)
                        }
                    }
                }
            }
        }
    }

    // Mark-paid is a real mutation (status → PAID, stamps today): confirm it and offer a way back.
    val onMarkPaid: (String) -> Unit = { id ->
        viewModel.markPaid(id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Pagado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.unmarkPaid(id)
        }
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
                    item {
                        // Title moved to the shell's app bar; the link to the schedule behind this
                        // timeline stays here, right-aligned where it was.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onOpenRecurring) { Text("Recurrentes") }
                        }
                    }

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
                                        canPay = !uiState.methodFor(payment)?.deepLinkPackage.isNullOrBlank(),
                                        onClick = {
                                            // A projection has no detail screen; open its template.
                                            if (payment.isProjected) {
                                                onOpenRecurring()
                                            } else {
                                                onPaymentClick(payment.transaction.id)
                                            }
                                        },
                                        onPay = { onPay(payment) },
                                        onMarkPaid = { onMarkPaid(payment.transaction.id) },
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
    canPay: Boolean,
    onClick: () -> Unit,
    onPay: () -> Unit,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit,
) {
    // Projections have no row to delete, so they get no swipe affordance at all — that is the whole
    // safety story: a gesture can never destroy a schedule from this screen.
    if (payment.isProjected) {
        UpcomingRow(payment = payment, canPay = canPay, onClick = onClick, onPay = onPay, onMarkPaid = onMarkPaid)
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
            canPay = canPay,
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
    canPay: Boolean,
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
        // Pay in the bank/wallet app (primary for a bill you actually owe); the check is the
        // "already paid / paid another way" shortcut and stays as the secondary action.
        if (canPay) {
            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPay()
                },
                modifier = Modifier.padding(start = Spacing.sm),
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Pagar en la app")
            }
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
