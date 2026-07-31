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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.designsystem.component.animatedItem
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SkeletonRows
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.component.ColorSwatchPicker
import pe.moneyflow.core.ui.component.IconChoicePicker
import pe.moneyflow.core.ui.component.SwatchPalette

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onDelete: (Category) -> Unit = { category ->
        viewModel.delete(category.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Categoría eliminada",
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
            if (uiState.isLoading) {
                item { SkeletonRows(modifier = Modifier.padding(top = Spacing.sm), count = 8) }
            } else if (uiState.isEmpty) {
                item {
                    MoneyCard(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                        EmptyState(
                            icon = Icons.Rounded.Category,
                            title = "Sin categorías",
                            subtitle = "Crea categorías para organizar en qué se va tu dinero.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                if (uiState.expense.isNotEmpty()) {
                    item { SectionHeader(title = "Gastos", modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) }
                    items(uiState.expense, key = { it.id }) { category ->
                        SwipeableCategory(
                            category = category,
                            onDelete = onDelete,
                            modifier = animatedItem(),
                        )
                    }
                }
                if (uiState.income.isNotEmpty()) {
                    item { SectionHeader(title = "Ingresos", modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) }
                    items(uiState.income, key = { it.id }) { category ->
                        SwipeableCategory(
                            category = category,
                            onDelete = onDelete,
                            modifier = animatedItem(),
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategorySheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, colorHex, iconKey, type ->
                viewModel.add(name, colorHex, iconKey, type)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun SwipeableCategory(
    category: Category,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(category)
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
            CategoryRow(category = category, onDelete = { onDelete(category) })
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
private fun AddCategorySheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, iconKey: String, type: CategoryType) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CategoryType.EXPENSE) }
    var colorHex by remember { mutableStateOf(SwatchPalette.first()) }
    var iconKey by remember { mutableStateOf(IconChoices.first()) }

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
                text = "Nueva categoría",
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

            Text("Color", style = MaterialTheme.typography.labelLarge)
            ColorSwatchPicker(selectedHex = colorHex, onSelect = { colorHex = it })

            Text("Ícono", style = MaterialTheme.typography.labelLarge)
            IconChoicePicker(
                choices = IconChoices,
                selectedKey = iconKey,
                onSelect = { iconKey = it },
            )

            Button(
                onClick = { onConfirm(name, colorHex, iconKey, type) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Agregar categoría") }
        }
    }
}
