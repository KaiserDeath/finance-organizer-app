package pe.moneyflow.feature.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.animatedItem
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.illustration.Illustration
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.MoneyProgressBar
import pe.moneyflow.core.designsystem.component.SkeletonBlocks
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.ui.util.toMonthNameOnly
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.ui.component.CategoryAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // null = closed; Editing(null) = creating; Editing(progress) = editing that budget.
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var consumedInitialEdit by remember { mutableStateOf(false) }

    // "Ajustar el límite" arrives with the budget id in the route: open its editor as soon as
    // the list has loaded, exactly once.
    LaunchedEffect(uiState.items, consumedInitialEdit) {
        if (consumedInitialEdit) return@LaunchedEffect
        val wanted = viewModel.initialEditBudgetId ?: return@LaunchedEffect
        val match = uiState.items.firstOrNull { it.budget.id == wanted }
        if (match != null) {
            editorTarget = EditorTarget(match)
            consumedInitialEdit = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editorTarget = EditorTarget(null) }) {
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
            if (uiState.isLoading) {
                item { SkeletonBlocks(heroHeight = 116.dp, count = 3, blockHeight = 148.dp) }
            } else if (uiState.isEmpty) {
                // Shown above the empty state too: with no category budgets yet, "you set aside X
                // and have assigned none of it" is precisely the thing the screen should say.
                if (uiState.hasRollup) {
                    item { MonthAllocationCard(state = uiState) }
                }
                item {
                    MoneyCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            illustration = Illustration.NoBudgets,
                            title = "Sin presupuestos",
                            subtitle = "Crea un límite mensual por categoría para controlar tus gastos.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                if (uiState.hasRollup) {
                    item { MonthAllocationCard(state = uiState) }
                }
                item {
                    OverallCard(
                        spentMinor = uiState.totalSpentMinor,
                        limitMinor = uiState.totalLimitMinor,
                        currencyCode = uiState.currencyCode,
                    )
                }
                items(uiState.items, key = { it.budget.id }) { progress ->
                    BudgetCard(
                        progress = progress,
                        onClick = { editorTarget = EditorTarget(progress) },
                        onDelete = { viewModel.delete(progress.budget.id) },
                        modifier = animatedItem(),
                    )
                }
            }
        }
    }

    editorTarget?.let { target ->
        BudgetEditorSheet(
            existing = target.progress,
            categories = uiState.expenseCategories,
            currencyCode = uiState.currencyCode,
            onDismiss = { editorTarget = null },
            onConfirm = { name, categoryId, amount, period ->
                val previous = target.progress?.budget
                viewModel.save(previous, name, categoryId, amount, period)
                editorTarget = null
                if (previous != null) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Límite actualizado",
                            actionLabel = "Deshacer",
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoEdit()
                    }
                }
            },
        )
    }
}

/** Wrapper so "open the create sheet" (null progress) is distinguishable from "closed". */
private data class EditorTarget(val progress: BudgetProgress?)

/**
 * How the month's budget has been divided up, above the per-category cards.
 *
 * The screen previously opened straight into the list, which answers "how is each category doing?"
 * but never "how much of my month have I actually allocated?" — the question you ask *before*
 * deciding a limit. It only appears when a month budget exists; without one there is nothing to
 * divide, and the category limits stand alone.
 *
 * The closing line exists to keep two numbers from looking like a contradiction: spending outside
 * every budget is normal, and the month total still counts it.
 */
@Composable
private fun MonthAllocationCard(state: BudgetsUiState) {
    val monthly = state.monthlyBudgetMinor ?: return
    val currency = state.currencyCode

    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Presupuesto de ${state.month.toMonthNameOnly()}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.format(monthly, currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(Spacing.lg))
        AllocationRow(
            label = when (state.items.size) {
                0 -> "Sin repartir en categorías"
                1 -> "Repartido en 1 categoría"
                else -> "Repartido en ${state.items.size} categorías"
            },
            amount = Money.format(state.totalLimitMinor, currency),
            accent = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.sm))
        AllocationRow(
            label = "Sin asignar a ninguna categoría",
            amount = Money.format(state.unassignedMinor ?: 0L, currency),
            accent = MaterialTheme.colorScheme.outline,
        )

        if (state.unbudgetedSpentMinor > 0) {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "De lo gastado este mes, " +
                    "${Money.format(state.unbudgetedSpentMinor, currency)} cayó fuera de todo " +
                    "presupuesto. El total del mes sigue siendo uno solo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AllocationRow(label: String, amount: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
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
        MoneyProgressBar(fraction = fraction, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BudgetCard(
    progress: BudgetProgress,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = when {
        progress.isOverBudget -> MaterialTheme.moneyColors.negative
        progress.isNearLimit -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val accent = colorFromHex(progress.category?.colorHex, MaterialTheme.colorScheme.primary)

    MoneyCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
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
        MoneyProgressBar(fraction = progress.fraction, color = barColor)
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
                    modifier = Modifier.size(IconSize.sm),
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
private fun BudgetEditorSheet(
    existing: BudgetProgress?,
    categories: List<pe.moneyflow.core.model.Category>,
    onDismiss: () -> Unit,
    currencyCode: String,
    onConfirm: (name: String, categoryId: String?, amountMinor: Long, period: BudgetPeriod) -> Unit,
) {
    val isEditing = existing != null
    var name by remember { mutableStateOf(existing?.budget?.name ?: "") }
    var amountText by remember {
        mutableStateOf(existing?.budget?.amountMinor?.let(Money::formatPlain) ?: "")
    }
    var categoryId by remember { mutableStateOf(existing?.budget?.categoryId) }
    var period by remember { mutableStateOf(existing?.budget?.period ?: BudgetPeriod.MONTHLY) }

    val amountMinor = Money.parseToMinor(amountText) ?: 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = if (isEditing) "Editar presupuesto" else "Nuevo presupuesto",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
                prefix = { Text("${Money.symbolFor(currencyCode)} ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Periodo", style = MaterialTheme.typography.labelLarge)
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

            Text("Categoría", style = MaterialTheme.typography.labelLarge)
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

            Button(
                onClick = { onConfirm(name, categoryId, amountMinor, period) },
                enabled = name.isNotBlank() && amountMinor > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (isEditing) "Guardar cambios" else "Crear presupuesto") }
        }
    }
}

private fun periodLabel(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.WEEKLY -> "Semanal"
    BudgetPeriod.MONTHLY -> "Mensual"
    BudgetPeriod.YEARLY -> "Anual"
}
