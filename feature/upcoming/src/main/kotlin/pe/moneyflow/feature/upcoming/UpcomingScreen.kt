package pe.moneyflow.feature.upcoming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.NegativeRed
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.util.toShortLabel

@Composable
fun UpcomingScreen(
    onPaymentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.EventAvailable,
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
                    Text(
                        text = "Próximos pagos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                uiState.sections.forEach { section ->
                    item(key = "header-${section.bucket}") {
                        SectionHeaderRow(
                            label = section.label,
                            total = Money.format(section.totalMinor, uiState.currencyCode),
                            isOverdue = section.bucket == UpcomingBucket.OVERDUE,
                        )
                    }
                    item(key = "card-${section.bucket}") {
                        MoneyCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = Spacing.xs),
                        ) {
                            section.items.forEach { payment ->
                                UpcomingRow(
                                    payment = payment,
                                    isOverdue = section.bucket == UpcomingBucket.OVERDUE,
                                    onClick = { onPaymentClick(payment.transaction.id) },
                                    onMarkPaid = { viewModel.markPaid(payment.transaction.id) },
                                )
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
            color = if (isOverdue) NegativeRed else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = total,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpcomingRow(
    payment: UpcomingPayment,
    isOverdue: Boolean,
    onClick: () -> Unit,
    onMarkPaid: () -> Unit,
) {
    val tx = payment.transaction
    val accent = colorFromHex(payment.category?.colorHex, MaterialTheme.colorScheme.primary)
    val dueLabel = tx.estimatedDate?.toShortLabel() ?: "Sin fecha"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(icon = iconForKey(payment.category?.iconKey ?: "category"), accent = accent)
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            Text(
                text = tx.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isOverdue) "Venció el $dueLabel" else dueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOverdue) NegativeRed else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = Money.format(tx.amountMinor, tx.currencyCode),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FilledTonalIconButton(
            onClick = onMarkPaid,
            modifier = Modifier.padding(start = Spacing.sm),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = "Marcar como pagado")
        }
    }
}
