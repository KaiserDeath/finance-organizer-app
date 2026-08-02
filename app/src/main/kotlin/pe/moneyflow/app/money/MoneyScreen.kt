package pe.moneyflow.app.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.ui.util.money

/**
 * "Tu dinero": the money destinations, each row carrying its live figure — not a mute chevron.
 * Settings lives behind its own single door at the bottom.
 */
@Composable
fun MoneyScreen(
    onOpenBudgets: () -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        item(key = "money") {
            MoneyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                MoneyRow(
                    icon = Icons.Rounded.TrackChanges,
                    title = "Presupuestos",
                    figure = if (state.budgetsAtRisk > 0) "${state.budgetsAtRisk} en riesgo" else "Todo en orden",
                    figureColor = if (state.budgetsAtRisk > 0) {
                        MaterialTheme.moneyColors.negative
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    figureDescription = if (state.budgetsAtRisk > 0) {
                        "${state.budgetsAtRisk} presupuesto(s) en riesgo"
                    } else {
                        "Ningún presupuesto en riesgo"
                    },
                    isLoading = state.isLoading,
                    onClick = onOpenBudgets,
                )
                MoneyDivider()
                MoneyRow(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Próximos pagos",
                    figure = money(state.upcomingTotalMinor, state.currencyCode),
                    caption = if (state.overdueCount > 0) "${state.overdueCount} vencido(s)" else null,
                    captionColor = MaterialTheme.moneyColors.negative,
                    figureDescription = buildString {
                        append("Pendiente ${money(state.upcomingTotalMinor, state.currencyCode)}")
                        if (state.overdueCount > 0) append(", ${state.overdueCount} pago(s) vencido(s)")
                    },
                    isLoading = state.isLoading,
                    onClick = onOpenUpcoming,
                )
                MoneyDivider()
                MoneyRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Cuentas",
                    figure = money(state.accountsBalanceMinor, state.currencyCode),
                    figureDescription =
                        "Saldo total ${money(state.accountsBalanceMinor, state.currencyCode)}",
                    isLoading = state.isLoading,
                    onClick = onOpenAccounts,
                )
                MoneyDivider()
                MoneyRow(
                    icon = Icons.Rounded.Savings,
                    title = "Ahorros",
                    figure = money(state.savingsBalanceMinor, state.currencyCode),
                    figureDescription =
                        "Ahorrado ${money(state.savingsBalanceMinor, state.currencyCode)}",
                    isLoading = state.isLoading,
                    onClick = onOpenSavings,
                )
                MoneyDivider()
                MoneyRow(
                    icon = Icons.Rounded.CreditCard,
                    title = "Métodos de pago",
                    figure = "${state.methodsCount} configurado(s)",
                    figureDescription = "${state.methodsCount} método(s) de pago configurado(s)",
                    isLoading = state.isLoading,
                    onClick = onOpenPaymentMethods,
                )
            }
        }
        item(key = "settings") {
            MoneyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                MoneyRow(
                    icon = Icons.Rounded.Settings,
                    title = "Ajustes",
                    figure = null,
                    showChevron = true,
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun MoneyDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 68.dp),
    )
}

@Composable
private fun MoneyRow(
    icon: ImageVector,
    title: String,
    figure: String?,
    onClick: () -> Unit,
    figureColor: Color = MaterialTheme.colorScheme.onSurface,
    figureDescription: String? = null,
    caption: String? = null,
    captionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isLoading: Boolean = false,
    showChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md),
        )
        if (figure != null && !isLoading) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = if (figureDescription != null) {
                    Modifier.semantics { contentDescription = figureDescription }
                } else {
                    Modifier
                },
            ) {
                Text(
                    text = figure,
                    style = MaterialTheme.typography.labelLarge,
                    color = figureColor,
                )
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = captionColor,
                    )
                }
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
