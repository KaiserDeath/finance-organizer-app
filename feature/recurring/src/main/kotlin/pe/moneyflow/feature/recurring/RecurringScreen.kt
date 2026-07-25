package pe.moneyflow.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Recurrentes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nueva plantilla recurrente")
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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item {
                Text(
                    text = "Estas plantillas generan tus pagos automáticamente en la fecha programada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.isEmpty) {
                item {
                    MoneyCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.EventRepeat,
                            title = "Sin recurrentes",
                            subtitle = "Crea plantillas para tu alquiler, suscripciones o sueldo.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                items(uiState.items, key = { it.id }) { item ->
                    RecurringCard(
                        item = item,
                        category = uiState.categories.firstOrNull { it.id == item.categoryId },
                        onToggleAuto = { viewModel.toggleAutoCreate(item) },
                        onDelete = { viewModel.delete(item.id) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringDialog(
            categories = uiState.categories,
            today = viewModel.today(),
            onDismiss = { showAddDialog = false },
            onConfirm = { title, categoryId, amount, frequency, startDate, autoCreate ->
                viewModel.add(title, categoryId, amount, frequency, startDate, autoCreate)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun RecurringCard(
    item: RecurringExpense,
    category: Category?,
    onToggleAuto: () -> Unit,
    onDelete: () -> Unit,
) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        append(frequencyLabel(item.frequency))
                        category?.let { append(" · ${it.name}") }
                        append(" · próximo ${item.nextRunDate}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Money.format(item.amountMinor, item.currencyCode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Generar automáticamente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(checked = item.autoCreate, onCheckedChange = { onToggleAuto() })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringDialog(
    categories: List<Category>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        categoryId: String?,
        amountMinor: Long,
        frequency: RecurrenceFrequency,
        startDate: LocalDate,
        autoCreate: Boolean,
    ) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var frequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var autoCreate by remember { mutableStateOf(true) }

    val amountMinor = Money.parseToMinor(amountText) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, categoryId, amountMinor, frequency, today, autoCreate) },
                enabled = title.isNotBlank() && amountMinor > 0,
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nueva plantilla recurrente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto") },
                    prefix = { Text("S/ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Frecuencia", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        RecurrenceFrequency.WEEKLY to "Semanal",
                        RecurrenceFrequency.MONTHLY to "Mensual",
                        RecurrenceFrequency.YEARLY to "Anual",
                    )
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(label) }
                    }
                }

                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("General") },
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Generar automáticamente", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoCreate, onCheckedChange = { autoCreate = it })
                }
            }
        },
    )
}

private fun frequencyLabel(frequency: RecurrenceFrequency): String = when (frequency) {
    RecurrenceFrequency.DAILY -> "Diario"
    RecurrenceFrequency.WEEKLY -> "Semanal"
    RecurrenceFrequency.MONTHLY -> "Mensual"
    RecurrenceFrequency.YEARLY -> "Anual"
}
