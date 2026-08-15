package pe.moneyflow.feature.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import pe.moneyflow.core.ui.safearea.safeArea
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
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
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.SavingsGoal
import pe.moneyflow.core.ui.util.money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var contributeGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onDelete: (SavingsGoal) -> Unit = { goal ->
        viewModel.delete(goal.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Meta eliminada",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.safeArea("snackbar_savings")) },
        topBar = {
            TopAppBar(
                title = { Text("Ahorros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nueva meta")
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
                item { SkeletonBlocks(heroHeight = 116.dp, count = 3, blockHeight = 132.dp) }
            } else if (uiState.isEmpty) {
                item {
                    MoneyCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            illustration = Illustration.NoSavings,
                            title = "Sin metas de ahorro",
                            subtitle = "Crea una meta (fondo de emergencia, un viaje…) y registra tus aportes.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                item {
                    OverallCard(
                        savedMinor = uiState.totalSavedMinor,
                        targetMinor = uiState.totalTargetMinor,
                        currencyCode = uiState.currencyCode,
                    )
                }
                items(uiState.goals, key = { it.id }) { goal ->
                    SwipeableGoal(
                        goal = goal,
                        currencyCode = uiState.currencyCode,
                        onContribute = { contributeGoal = goal },
                        onDelete = onDelete,
                        modifier = animatedItem(),
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalSheet(
            currencyCode = uiState.currencyCode,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target ->
                viewModel.add(name, target, null)
                showAddDialog = false
            },
        )
    }

    contributeGoal?.let { goal ->
        ContributeDialog(
            goal = goal,
            currencyCode = uiState.currencyCode,
            onDismiss = { contributeGoal = null },
            onConfirm = { delta ->
                viewModel.contribute(goal.id, delta)
                contributeGoal = null
            },
        )
    }
}

@Composable
private fun OverallCard(savedMinor: Long, targetMinor: Long, currencyCode: String) {
    val fraction = if (targetMinor > 0) (savedMinor.toFloat() / targetMinor).coerceIn(0f, 1f) else 0f
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Ahorrado en total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${money(savedMinor, currencyCode)} / ${money(targetMinor, currencyCode)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        MoneyProgressBar(fraction = fraction, color = MaterialTheme.moneyColors.positive)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGoal(
    goal: SavingsGoal,
    currencyCode: String,
    onContribute: () -> Unit,
    onDelete: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(goal)
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
        GoalCard(
            goal = goal,
            currencyCode = currencyCode,
            onContribute = onContribute,
            onDelete = { onDelete(goal) },
        )
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
private fun GoalCard(
    goal: SavingsGoal,
    currencyCode: String,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        MoneyCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (goal.isComplete) {
                            "¡Meta alcanzada!"
                        } else {
                            "Faltan ${money(goal.remainingMinor, currencyCode)}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (goal.isComplete) MaterialTheme.moneyColors.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.safeArea("action_delete_savings_${goal.id}"),
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            MoneyProgressBar(fraction = goal.fraction, color = MaterialTheme.moneyColors.positive)
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${money(goal.currentAmountMinor, currencyCode)} de ${money(goal.targetAmountMinor, currencyCode)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(
                    onClick = onContribute,
                    label = { Text("Aportar") },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetMinor: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    val targetMinor = Money.parseToMinor(targetText) ?: 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.safeArea("sheet_savings_goal_editor"),
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
                text = "Nueva meta",
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
                value = targetText,
                onValueChange = { targetText = it },
                label = { Text("Monto objetivo") },
                prefix = { Text("${Money.symbolFor(currencyCode)} ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onConfirm(name, targetMinor) },
                enabled = name.isNotBlank() && targetMinor > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Crear meta") }
        }
    }
}

@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (deltaMinor: Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var withdraw by remember { mutableStateOf(false) }
    val amountMinor = Money.parseToMinor(amountText) ?: 0L

    AlertDialog(
        modifier = Modifier.safeArea("dialog_savings_adjustment"),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (withdraw) -amountMinor else amountMinor) },
                enabled = amountMinor > 0,
            ) { Text(if (withdraw) "Retirar" else "Aportar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(goal.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AssistChip(
                        onClick = { withdraw = false },
                        label = { Text("Aportar") },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    )
                    AssistChip(
                        onClick = { withdraw = true },
                        label = { Text("Retirar") },
                        leadingIcon = { Icon(Icons.Rounded.Remove, contentDescription = null) },
                    )
                }
                Text(
                    text = if (withdraw) "Vas a retirar de esta meta." else "Vas a aportar a esta meta.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }
        },
    )
}
