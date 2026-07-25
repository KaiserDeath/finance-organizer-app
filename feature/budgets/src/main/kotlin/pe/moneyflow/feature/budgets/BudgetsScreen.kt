package pe.moneyflow.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.ui.component.CategoryAvatar

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo presupuesto")
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
                    text = "Presupuestos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (uiState.isEmpty) {
                item {
                    MoneyCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.Savings,
                            title = "Sin presupuestos",
                            subtitle = "Crea un límite mensual por categoría para controlar tus gastos.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                item {
                    OverallCard(
                        spentMinor = uiState.totalSpentMinor,
                        limitMinor = uiState.totalLimitMinor,
                        currencyCode = uiState.currencyCode,
                    )
                }
                items(uiState.items, key = { it.budget.id }) { progress ->
                    BudgetCard(progress = progress, onDelete = { viewModel.delete(progress.budget.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            categories = uiState.expenseCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, categoryId, amount, period ->
                viewModel.add(name, categoryId, amount, period)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun OverallCard(spentMinor: Long, limitMinor: Long, currencyCode: String) {
    val fraction = if (limitMinor > 0) (spentMinor.toFloat() / limitMinor).coerceIn(0f, 1f) else 0f
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Gastado de tus presupuestos",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${Money.format(spentMinor, currencyCode)} / ${Money.format(limitMinor, currencyCode)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun BudgetCard(progress: BudgetProgress, onDelete: () -> Unit) {
    val barColor = when {
        progress.isOverBudget -> NegativeRed
        progress.isNearLimit -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val accent = colorFromHex(progress.category?.colorHex, MaterialTheme.colorScheme.primary)

    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(
                icon = iconForKey(progress.category?.iconKey ?: "savings"),
                accent = accent,
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text(
                    text = progress.budget.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = periodLabel(progress.budget.period),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${Money.format(progress.spentMinor, progress.currencyCode)} de ${Money.format(progress.budget.amountMinor, progress.currencyCode)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${progress.percentUsed}%",
                style = MaterialTheme.typography.labelLarge,
                color = barColor,
            )
        }

        if (progress.isOverBudget || progress.isNearLimit) {
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.height(16.dp),
                )
                Spacer(Modifier.padding(horizontal = Spacing.xxs))
                Text(
                    text = if (progress.isOverBudget) {
                        "Superaste el límite por ${Money.format(-progress.remainingMinor, progress.currencyCode)}"
                    } else {
                        "Te quedan ${Money.format(progress.remainingMinor, progress.currencyCode)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = barColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    categories: List<pe.moneyflow.core.model.Category>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, categoryId: String?, amountMinor: Long, period: BudgetPeriod) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }

    val amountMinor = Money.parseToMinor(amountText) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, categoryId, amountMinor, period) },
                enabled = name.isNotBlank() && amountMinor > 0,
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nuevo presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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

                Text("Periodo", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val periods = listOf(
                        BudgetPeriod.WEEKLY to "Semanal",
                        BudgetPeriod.MONTHLY to "Mensual",
                        BudgetPeriod.YEARLY to "Anual",
                    )
                    periods.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = period == value,
                            onClick = { period = value },
                            shape = SegmentedButtonDefaults.itemShape(index, periods.size),
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
            }
        },
    )
}

private fun periodLabel(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.WEEKLY -> "Semanal"
    BudgetPeriod.MONTHLY -> "Mensual"
    BudgetPeriod.YEARLY -> "Anual"
}
