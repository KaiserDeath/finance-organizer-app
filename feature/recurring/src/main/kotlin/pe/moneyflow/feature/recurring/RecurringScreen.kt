package pe.moneyflow.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.animatedItem
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SkeletonBlocks
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.ui.component.PaymentStatusPill
import pe.moneyflow.core.ui.paymentmethod.PaymentMethodSelector
import pe.moneyflow.core.ui.paymentmethod.toCardKind
import pe.moneyflow.core.ui.paymentmethod.toNature
import pe.moneyflow.core.ui.recurrence.RecurrenceEditor
import pe.moneyflow.core.ui.recurrence.RecurrenceMode
import pe.moneyflow.core.ui.recurrence.RecurrenceValue
import pe.moneyflow.core.ui.recurrence.recurrenceSummary
import pe.moneyflow.core.ui.util.toShortLabel
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // null = sheet closed; NewTemplate sentinel handled by showSheet + editing.
    var editing by remember { mutableStateOf<RecurringExpense?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // "Pagar este mes" resolves an occurrence in the VM, then routes to the transaction editor.
    LaunchedEffect(Unit) {
        viewModel.openTransaction.collect { id -> onOpenTransaction(id) }
    }

    val onDelete: (RecurringExpense) -> Unit = { item ->
        viewModel.delete(item.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Recurrente eliminado",
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
                title = { Text("Recurrentes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showSheet = true
            }) {
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
                    text = "Tus gastos mensuales: cuánto ya pagaste y cuánto falta este mes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.isLoading) {
                item { SkeletonBlocks(count = 3, blockHeight = 196.dp) }
            } else if (uiState.isEmpty) {
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
                items(uiState.items, key = { it.template.id }) { item ->
                    SwipeableRecurring(
                        item = item,
                        onTogglePaid = { viewModel.setPaidThisMonth(item.template, !item.paidThisMonth) },
                        onEdit = {
                            editing = item.template
                            showSheet = true
                        },
                        onPayThisMonth = { viewModel.payThisMonth(item.template) },
                        onToggleAuto = { viewModel.toggleAutoCreate(item.template) },
                        onDelete = onDelete,
                        modifier = animatedItem(),
                    )
                }
            }
        }
    }

    if (showSheet) {
        AddEditRecurringSheet(
            existing = editing,
            categories = uiState.categories,
            paymentMethods = uiState.paymentMethods,
            accounts = uiState.accounts,
            today = viewModel.today(),
            currencyCode = uiState.currencyCode,
            onDismiss = { showSheet = false },
            onConfirm = { template ->
                viewModel.save(template)
                showSheet = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRecurring(
    item: RecurringItemUi,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onPayThisMonth: () -> Unit,
    onToggleAuto: () -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(item.template)
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
        RecurringCard(
            item = item,
            onTogglePaid = onTogglePaid,
            onEdit = onEdit,
            onPayThisMonth = onPayThisMonth,
            onToggleAuto = onToggleAuto,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringCard(
    item: RecurringItemUi,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onPayThisMonth: () -> Unit,
    onToggleAuto: () -> Unit,
) {
    val template = item.template
    val haptics = LocalHapticFeedback.current
    Surface(color = MaterialTheme.colorScheme.surface) {
        MoneyCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildString {
                            append(scheduleLabel(template))
                            item.category?.let { append(" · ${it.name}") }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = Money.format(template.amountMinor, template.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                PaymentStatusPill(display = item.displayStatus, showPaid = true)
                item.paymentMethod?.let { MethodPill(it) }
                Text(
                    text = dueLabel(item),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            // Primary status action: a self-describing labeled toggle, so tapping to settle (or
            // un-settle) this month reads clearly instead of relying on a bare icon.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (item.paidThisMonth) {
                    FilledTonalButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePaid()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Pagado")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePaid()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Marcar pagado")
                    }
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Editar")
                }
            }

            // Secondary: pay this month's charge with a different method (opens the tx editor).
            OutlinedButton(
                onClick = onPayThisMonth,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) {
                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Pagar con otro método")
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
                Switch(checked = template.autoCreate, onCheckedChange = { onToggleAuto() })
            }
        }
    }
}

/** Small tonal chip naming the template's default payment method. */
@Composable
private fun MethodPill(method: PaymentMethod) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconForKey(method.iconKey),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = method.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** "Vence en 6 días · 1 ago" / "Vencido hace 2 días · 24 jul" / "Próximo pago 1 ago" once paid. */
private fun dueLabel(item: RecurringItemUi): String {
    val date = item.template.nextRunDate.toShortLabel()
    if (item.paidThisMonth) return "Próximo pago $date"
    val days = item.daysUntilDue
    val relative = when {
        days < 0L -> "Vencido hace ${-days} ${dayWord(-days)}"
        days == 0L -> "Vence hoy"
        days == 1L -> "Vence mañana"
        else -> "Vence en $days días"
    }
    return "$relative · $date"
}

private fun dayWord(days: Long): String = if (days == 1L) "día" else "días"

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRecurringSheet(
    existing: RecurringExpense?,
    categories: List<Category>,
    paymentMethods: List<PaymentMethod>,
    accounts: List<Account>,
    today: LocalDate,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (RecurringExpense) -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountText by remember {
        mutableStateOf(existing?.let { Money.formatPlain(it.amountMinor) } ?: "")
    }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var paymentMethodId by remember { mutableStateOf(existing?.paymentMethodId) }
    var cardKind by remember { mutableStateOf(existing?.cardKind) }
    var accountId by remember { mutableStateOf(existing?.accountId) }
    var autoCreate by remember { mutableStateOf(existing?.autoCreate ?: true) }
    var recurrence by remember { mutableStateOf(existing?.toRecurrenceValue() ?: RecurrenceValue()) }

    val amountMinor = Money.parseToMinor(amountText) ?: 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canSave = title.isNotBlank() && amountMinor > 0 && recurrence.isValid

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
                text = if (existing == null) "Nueva plantilla recurrente" else "Editar plantilla",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
                prefix = { Text("${Money.symbolFor(currencyCode)} ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("¿Cada cuánto se repite?", style = MaterialTheme.typography.labelLarge)
            RecurrenceEditor(value = recurrence, onValueChange = { recurrence = it })

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

            if (paymentMethods.isNotEmpty()) {
                Text("Método de pago", style = MaterialTheme.typography.labelLarge)
                PaymentMethodSelector(
                    methods = paymentMethods,
                    selectedId = paymentMethodId,
                    selectedNature = cardKind.toNature(),
                    onSelect = { id, nature ->
                        paymentMethodId = id
                        cardKind = if (id == null) null else nature.toCardKind()
                        // Keep the account in step with the method's linked account.
                        paymentMethods.firstOrNull { it.id == id }?.accountId?.let { accountId = it }
                    },
                    allowNone = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (accounts.isNotEmpty()) {
                Text("Cuenta", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = accountId == null,
                        onClick = { accountId = null },
                        label = { Text("Ninguna") },
                    )
                    accounts.forEach { account ->
                        FilterChip(
                            selected = accountId == account.id,
                            onClick = { accountId = account.id },
                            label = { Text(account.name) },
                        )
                    }
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

            Button(
                onClick = {
                    val base = existing ?: RecurringExpense(
                        id = UUID.randomUUID().toString(),
                        title = "",
                        amountMinor = 0,
                        nextRunDate = today,
                    )
                    onConfirm(
                        base.copy(
                            title = title.trim(),
                            amountMinor = amountMinor,
                            categoryId = categoryId,
                            paymentMethodId = paymentMethodId,
                            cardKind = cardKind,
                            accountId = accountId,
                            frequency = recurrence.frequency,
                            interval = recurrence.effectiveInterval,
                            daysOfMonth = recurrence.days,
                            // Keep an existing schedule's next run; seed a new one from the start date.
                            nextRunDate = existing?.nextRunDate
                                ?: RecurringExpense.firstRunOnOrAfter(today, recurrence.days),
                            autoCreate = autoCreate,
                        ),
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (existing == null) "Crear recurrente" else "Guardar cambios") }
        }
    }
}

/** Rebuilds the editor's hoisted recurrence state from a saved template, for edit mode. */
private fun RecurringExpense.toRecurrenceValue(): RecurrenceValue =
    if (daysOfMonth.isEmpty()) {
        RecurrenceValue(mode = RecurrenceMode.INTERVAL, interval = interval, unit = frequency)
    } else {
        RecurrenceValue(
            mode = RecurrenceMode.DAYS_OF_MONTH,
            daysOfMonth = daysOfMonth.toSet(),
            monthStride = interval,
        )
    }

/** Human-readable schedule summary shown on each recurring card. */
private fun scheduleLabel(item: RecurringExpense): String =
    recurrenceSummary(item.daysOfMonth, item.frequency, item.interval)
