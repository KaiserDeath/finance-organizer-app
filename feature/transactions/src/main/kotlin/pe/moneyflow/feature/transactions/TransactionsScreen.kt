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
import androidx.compose.material3.FilledTonalIconButton
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                    onQueryChange = viewModel::onQueryChange,
                    onOpenFilters = { showFilters = true },
                )
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = uiState,
            onToggleType = viewModel::toggleType,
            onToggleCategory = viewModel::toggleCategory,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun TransactionsList(
    state: TransactionsUiState,
    onTransactionClick: (String) -> Unit,
    onDelete: (Transaction) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp, top = Spacing.md),
    ) {
        // Title now lives in the shell's collapsing app bar, not as a scrolling list item.
        item(key = "search") {
            SearchRow(
                query = state.filter.query,
                activeFilterCount = state.filter.types.size + state.filter.categoryIds.size,
                onQueryChange = onQueryChange,
                onOpenFilters = onOpenFilters,
            )
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
                    total = Money.format(section.expenseTotalMinor, state.currencyCode),
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
private fun SearchRow(
    query: String,
    activeFilterCount: Int,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
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
            modifier = Modifier.weight(1f),
        )
        BadgedBox(
            badge = { if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString()) } },
        ) {
            FilledTonalIconButton(onClick = onOpenFilters) {
                Icon(Icons.Rounded.Tune, contentDescription = "Filtros")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    state: TransactionsUiState,
    onToggleType: (TransactionType) -> Unit,
    onToggleCategory: (String) -> Unit,
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

            Text("Tipo", style = MaterialTheme.typography.labelLarge)
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

            if (state.expenseCategories.isNotEmpty()) {
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    state.expenseCategories.forEach { category ->
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
