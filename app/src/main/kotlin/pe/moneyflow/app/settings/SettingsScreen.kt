package pe.moneyflow.app.settings

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.ThemeMode

private data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/**
 * Configuration behind one door, split by the only question that matters here:
 * is this about the app, or about your data?
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenLegal: () -> Unit,
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
) {
    val appearance by appearanceViewModel.uiState.collectAsStateWithLifecycle()
    val appItems = listOf(
        SettingsItem(
            Icons.Rounded.Category,
            "Categorías",
            "Crea y organiza tus categorías",
            onOpenCategories,
        ),
        SettingsItem(
            Icons.Rounded.CurrencyExchange,
            "Moneda",
            "Moneda base y tipos de cambio",
            onOpenCurrency,
        ),
    )
    val dataItems = listOf(
        SettingsItem(
            Icons.Rounded.EventRepeat,
            "Pagos recurrentes",
            "Plantillas que se generan solas",
            onOpenRecurring,
        ),
        SettingsItem(
            Icons.Rounded.Backup,
            "Copia de seguridad",
            "Exporta o restaura tus datos",
            onOpenBackup,
        ),
        SettingsItem(
            Icons.Rounded.Lock,
            "Seguridad",
            "Bloqueo con PIN y biometría",
            onOpenSecurity,
        ),
        SettingsItem(
            Icons.Rounded.Info,
            "Acerca de y legal",
            "Privacidad, datos y marcas",
            onOpenLegal,
        ),
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            item(key = "app") {
                // The theme picker used to be a row leading to a screen that held nothing else.
                // It is three mutually exclusive options, so it fits where it is decided.
                SettingsGroup(label = "La app") {
                    ThemeCard(
                        selected = appearance.themeMode,
                        onSelect = appearanceViewModel::setThemeMode,
                    )
                    SettingsCard(items = appItems)
                }
            }
            item(key = "data") {
                SettingsGroup(label = "Tus datos") {
                    SettingsCard(items = dataItems)
                }
            }
        }
    }
}

/** A labelled group. Holds one or more cards, so a control can sit beside the rows it belongs with. */
@Composable
private fun SettingsGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
        content()
    }
}

@Composable
private fun SettingsCard(items: List<SettingsItem>) {
    MoneyCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = Spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            SettingsRow(item)
            if (index < items.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 68.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    MoneyCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
        Text(
            text = "Tema",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Elige claro, oscuro o seguir el sistema.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        // No dynamic-color switch: the brand palette is the app's identity, not a preference.
        // See MoneyFlowTheme's KDoc for why it was removed.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = listOf(
                ThemeMode.SYSTEM to "Sistema",
                ThemeMode.LIGHT to "Claro",
                ThemeMode.DARK to "Oscuro",
            )
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun SettingsRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
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
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.subtitle,
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
