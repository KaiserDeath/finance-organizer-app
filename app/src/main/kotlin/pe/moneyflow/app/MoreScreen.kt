package pe.moneyflow.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
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

@Composable
fun MoreScreen(
    onOpenCategories: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = 96.dp,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.lg),
    ) {
        item {
            Text(
                text = "Más",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            MoneyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                MoreRow(
                    icon = Icons.Rounded.Category,
                    title = "Categorías",
                    subtitle = "Crea y organiza tus categorías",
                    onClick = onOpenCategories,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 68.dp),
                )
                MoreRow(
                    icon = Icons.Rounded.CreditCard,
                    title = "Métodos de pago",
                    subtitle = "Tarjetas, Yape, Plin y bancos",
                    onClick = onOpenPaymentMethods,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 68.dp),
                )
                MoreRow(
                    icon = Icons.Rounded.EventRepeat,
                    title = "Pagos recurrentes",
                    subtitle = "Plantillas que se generan solas",
                    onClick = onOpenRecurring,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 68.dp),
                )
                MoreRow(
                    icon = Icons.Rounded.BarChart,
                    title = "Análisis y reportes",
                    subtitle = "Tendencias, comparativas y exportar CSV",
                    onClick = onOpenAnalytics,
                )
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
