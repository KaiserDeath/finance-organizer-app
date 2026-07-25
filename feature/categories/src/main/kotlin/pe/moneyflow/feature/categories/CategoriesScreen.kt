package pe.moneyflow.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.ui.component.CategoryAvatar

private val PaletteHex = listOf(
    "#4F46E5", "#0EA5A5", "#F59E0B", "#EC4899", "#8B5CF6", "#10B981",
    "#EF4444", "#3B82F6", "#F97316", "#14B8A6", "#A855F7", "#84CC16",
)
private val IconChoices = listOf(
    "food", "restaurant", "transport", "fuel", "home", "shopping",
    "health", "movie", "school", "bolt", "pets", "flight",
    "payments", "gift", "savings", "category",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nueva categoría")
            }
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
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (uiState.expense.isNotEmpty()) {
                item { SectionHeader(title = "Gastos", modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) }
                items(uiState.expense, key = { it.id }) { category ->
                    CategoryRow(category = category, onDelete = { viewModel.delete(category.id) })
                }
            }
            if (uiState.income.isNotEmpty()) {
                item { SectionHeader(title = "Ingresos", modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) }
                items(uiState.income, key = { it.id }) { category ->
                    CategoryRow(category = category, onDelete = { viewModel.delete(category.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, colorHex, iconKey, type ->
                viewModel.add(name, colorHex, iconKey, type)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconForKey(category.iconKey),
            accent = colorFromHex(category.colorHex, MaterialTheme.colorScheme.primary),
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.md),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, iconKey: String, type: CategoryType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CategoryType.EXPENSE) }
    var colorHex by remember { mutableStateOf(PaletteHex.first()) }
    var iconKey by remember { mutableStateOf(IconChoices.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, colorHex, iconKey, type) },
                enabled = name.isNotBlank(),
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf(CategoryType.EXPENSE to "Gasto", CategoryType.INCOME to "Ingreso")
                    types.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = type == value,
                            onClick = { type = value },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        ) { Text(label) }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PaletteHex.forEach { hex ->
                        val color = colorFromHex(hex, MaterialTheme.colorScheme.primary)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (colorHex == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { colorHex = hex },
                        )
                    }
                }

                Text("Ícono", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    IconChoices.forEach { key ->
                        val selected = iconKey == key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconForKey(key),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}
