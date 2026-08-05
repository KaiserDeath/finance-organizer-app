package pe.moneyflow.feature.transactions

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.animatedItem
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.illustration.Illustration
import pe.moneyflow.core.designsystem.component.ShimmerBox
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.component.TransactionRow
import pe.moneyflow.core.ui.util.money

@Composable
fun TransactionsScreen(
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFilters by remember { mutableStateOf(false) }

    val onDelete: (Transaction) -> Unit = { tx ->
        viewModel.delete(tx.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Movimiento eliminado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // Search and filters live in a fixed header above the list, never inside it: a filter
        // whose state scrolls out of view is a filter the user believes they didn't apply.
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val showHeader = !uiState.isLoading && !(uiState.isEmpty && !uiState.isFilterActive)
            if (showHeader) {
                FiltersHeader(
                    state = uiState,
                    onQueryChange = viewModel::onQueryChange,
                    onToggleCategory = viewModel::toggleCategory,
                    onOpenFilters = { showFilters = true },
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    uiState.isLoading -> LoadingList()
                    uiState.isEmpty && !uiState.isFilterActive -> EmptyState(
                        illustration = Illustration.NoTransactions,
                        title = "Sin movimientos",
                        subtitle = "Registra un gasto con el botón + para verlo aquí.",
                        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
                    )

                    else -> TransactionsList(
                        state = uiState,
                        onTransactionClick = onTransactionClick,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = uiState,
            onToggleType = viewModel::toggleType,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun FiltersHeader(
    state: TransactionsUiState,
    onQueryChange: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Column {
        SearchRow(
            query = state.filter.query,
            // The type filters stay behind the Tune sheet; the badge only counts those now that
            // category chips are visible on the header itself.
            activeFilterCount = state.filter.types.size,
            onQueryChange = onQueryChange,
            onOpenFilters = onOpenFilters,
        )
        if (state.expenseCategories.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "Categorías rápidas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item(key = "all") {
                        val noneSelected = state.filter.categoryIds.isEmpty()
                        FilterChip(
                            selected = noneSelected,
                            onClick = {
                                // Deselect every active category; the ViewModel API stays untouched.
                                state.filter.categoryIds.forEach(onToggleCategory)
                            },
                            label = { Text("Todas") },
                        )
                    }
                    items(items = state.expenseCategories, key = { it.id }) { category ->
                        FilterChip(
                            selected = category.id in state.filter.categoryIds,
                            onClick = { onToggleCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsList(
    state: TransactionsUiState,
    onTransactionClick: (String) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp, top = Spacing.md),
    ) {
        val movementCount = state.sections.sumOf { it.items.size }
        if (!state.isFilterActive && movementCount in 1..2) {
            item(key = "low-data-hint") {
                LowDataHint(count = movementCount)
            }
        }
        if (state.sections.isEmpty()) {
            item(key = "no-matches") {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Sin resultados",
                    subtitle = "Ningún movimiento coincide con tu búsqueda o filtros.",
                    modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                )
            }
        }

        state.sections.forEach { section ->
            item(key = "header-${section.dateLabel}") {
                SectionHeaderRow(
                    label = section.dateLabel,
                    total = money(section.expenseTotalMinor, state.currencyCode),
                )
            }
            items(items = section.items, key = { it.id }) { tx ->
                SwipeableTransaction(
                    transaction = tx,
                    category = tx.categoryId?.let { state.categoriesById[it] },
                    onClick = { onTransactionClick(tx.id) },
                    onDelete = onDelete,
                    modifier = animatedItem(),
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 72.dp),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SearchRow(
    query: String,
    activeFilterCount: Int,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Buscar") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            // FlowRow wraps this field and the filter button onto separate lines when the
            // available width or font scale makes the compact row unsafe.
            modifier = Modifier.weight(1f),
        )
        BadgedBox(
            badge = { if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString()) } },
        ) {
            FilledTonalButton(
                onClick = onOpenFilters,
                contentPadding = PaddingValues(horizontal = Spacing.md),
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null)
                Text("Filtros", modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    state: TransactionsUiState,
    onToggleType: (TransactionType) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filtros",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.isFilterActive) {
                    TextButton(onClick = onClearFilters) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                        Text("  Limpiar")
                    }
                }
            }

            // Category chips moved to the fixed header, where their state is always visible;
            // the sheet keeps the broader movement-type filter.
            Text("Tipo de movimiento", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                val typeLabels = listOf(
                    TransactionType.EXPENSE to "Gastos",
                    TransactionType.INCOME to "Ingresos",
                    TransactionType.TRANSFER to "Transferencias",
                )
                typeLabels.forEach { (type, label) ->
                    FilterChip(
                        selected = type in state.filter.types,
                        onClick = { onToggleType(type) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LowDataHint(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = if (count == 1) {
                "Tienes 1 movimiento. Añade otro para empezar a ver patrones."
            } else {
                "Tienes $count movimientos. Añade más para comparar tus días."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

@Composable
private fun SectionHeaderRow(label: String, total: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = total,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwipeableTransaction(
    transaction: Transaction,
    category: pe.moneyflow.core.model.Category?,
    onClick: () -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(transaction)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TransactionRow(
                transaction = transaction,
                category = category,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = "Eliminar",
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun LoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(count = 8) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp))
        }
    }
}
