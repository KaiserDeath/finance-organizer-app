package pe.moneyflow.feature.paymentmethods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.PaymentMethodType
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.util.launchPaymentApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentMethodsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Métodos de pago") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                    text = "Toca un método para abrir su app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                MoneyCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = Spacing.xs),
                ) {
                    uiState.methods.forEachIndexed { index, method ->
                        PaymentMethodRow(
                            method = method,
                            onClick = {
                                launchPaymentApp(
                                    context = context,
                                    packageName = method.deepLinkPackage,
                                    appName = method.name,
                                    playStoreId = method.playStoreId,
                                )
                            },
                        )
                        if (index < uiState.methods.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 72.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(method: PaymentMethod, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconForKey(method.iconKey),
            accent = colorFromHex(method.colorHex, MaterialTheme.colorScheme.primary),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            Text(
                text = method.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = method.type.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!method.deepLinkPackage.isNullOrBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Abrir app",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

private fun PaymentMethodType.label(): String = when (this) {
    PaymentMethodType.CASH -> "Efectivo"
    PaymentMethodType.CARD -> "Tarjeta"
    PaymentMethodType.EWALLET -> "Billetera digital"
    PaymentMethodType.BANK -> "Banco"
}
