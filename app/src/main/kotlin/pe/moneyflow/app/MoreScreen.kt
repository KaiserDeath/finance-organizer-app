package pe.moneyflow.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.Spacing

private data class MoreItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

private data class MoreSection(
    val label: String,
    val items: List<MoreItem>,
)

@Composable
fun MoreScreen(
    onOpenInsights: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenLegal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = listOf(
        MoreSection(
            label = "Tu dinero",
            items = listOf(
                MoreItem(
                    Icons.Rounded.Lightbulb,
                    "Sugerencias",
                    "Ideas inteligentes sobre tus finanzas",
                    onOpenInsights,
                ),
                MoreItem(
                    Icons.Rounded.AccountBalanceWallet,
                    "Cuentas",
                    "Saldos, transferencias y patrimonio neto",
                    onOpenAccounts,
                ),
                MoreItem(
                    Icons.Rounded.Savings,
                    "Ahorros",
                    "Metas de ahorro y aportes",
                    onOpenSavings,
                ),
                MoreItem(
                    Icons.Rounded.AccountBalance,
                    "Presupuestos",
                    "Límites de gasto por categoría",
                    onOpenBudgets,
                ),
            ),
        ),
        MoreSection(
            label = "Organización",
            items = listOf(
                MoreItem(
                    Icons.Rounded.Category,
                    "Categorías",
                    "Crea y organiza tus categorías",
                    onOpenCategories,
                ),
                MoreItem(
                    Icons.Rounded.CreditCard,
                    "Métodos de pago",
                    "Tarjetas, Yape, Plin y bancos",
                    onOpenPaymentMethods,
                ),
                MoreItem(
                    Icons.Rounded.EventRepeat,
                    "Pagos recurrentes",
                    "Plantillas que se generan solas",
                    onOpenRecurring,
                ),
                MoreItem(
                    Icons.Rounded.CurrencyExchange,
                    "Monedas",
                    "Moneda base y tipos de cambio",
                    onOpenCurrency,
                ),
            ),
        ),
        MoreSection(
            label = "Ajustes",
            items = listOf(
                MoreItem(
                    Icons.Rounded.Palette,
                    "Apariencia",
                    "Tema, modo oscuro y color dinámico",
                    onOpenAppearance,
                ),
                MoreItem(
                    Icons.Rounded.Lock,
                    "Seguridad",
                    "Bloqueo con PIN y biometría",
                    onOpenSecurity,
                ),
                MoreItem(
                    Icons.Rounded.Backup,
                    "Copia de seguridad",
                    "Exporta o restaura tus datos",
                    onOpenBackup,
                ),
                MoreItem(
                    Icons.Rounded.Info,
                    "Acerca de y legal",
                    "Privacidad, datos y marcas",
                    onOpenLegal,
                ),
            ),
        ),
    )

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
        item {
            Text(
                text = "Más",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        sections.forEach { section ->
            item(key = section.label) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.xs),
                    )
                    MoneyCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = Spacing.xs),
                    ) {
                        section.items.forEachIndexed { index, item ->
                            MoreRow(
                                icon = item.icon,
                                title = item.title,
                                subtitle = item.subtitle,
                                onClick = item.onClick,
                            )
                            if (index < section.items.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = 68.dp),
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
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
