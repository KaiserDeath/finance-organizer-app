package pe.moneyflow.feature.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.Priority
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.util.toFullLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar movimiento" else "Nuevo movimiento") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = uiState.canSave) {
                        Text("Guardar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.xs))

            // Type toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(TransactionType.EXPENSE to "Gasto", TransactionType.INCOME to "Ingreso")
                types.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    ) { Text(label) }
                }
            }

            // Status: paid now vs pending (an upcoming payment)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val statuses = listOf(
                    TransactionStatus.PAID to "Pagado",
                    TransactionStatus.PENDING to "Pendiente",
                )
                statuses.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = uiState.status == value,
                        onClick = { viewModel.onStatusChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
                    ) { Text(label) }
                }
            }

            // Amount
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Monto") },
                prefix = { Text("S/ ") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Descripción") },
                placeholder = { Text("Ej. Almuerzo, Uber, Netflix") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Category
            FieldLabel("Categoría")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                uiState.categories.forEach { category ->
                    FilterChip(
                        selected = uiState.categoryId == category.id,
                        onClick = { viewModel.onCategorySelect(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconForKey(category.iconKey),
                                contentDescription = null,
                                modifier = Modifier.height(18.dp),
                            )
                        },
                    )
                }
            }

            // Payment method
            FieldLabel("Método de pago")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                uiState.paymentMethods.forEach { method ->
                    FilterChip(
                        selected = uiState.paymentMethodId == method.id,
                        onClick = { viewModel.onPaymentMethodSelect(method.id) },
                        label = { Text(method.name) },
                    )
                }
            }

            // Account (drives account balances & net worth)
            if (uiState.accounts.isNotEmpty()) {
                FieldLabel("Cuenta")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    uiState.accounts.forEach { account ->
                        FilterChip(
                            selected = uiState.accountId == account.id,
                            onClick = { viewModel.onAccountSelect(account.id) },
                            label = { Text(account.name) },
                        )
                    }
                }
            }

            // Priority
            FieldLabel("Prioridad")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                val priorities = listOf(
                    Priority.LOW to "Baja",
                    Priority.NORMAL to "Normal",
                    Priority.HIGH to "Alta",
                )
                priorities.forEach { (value, label) ->
                    FilterChip(
                        selected = uiState.priority == value,
                        onClick = { viewModel.onPriorityChange(value) },
                        label = { Text(label) },
                    )
                }
            }

            // Date
            FieldLabel(if (uiState.isPending) "Fecha de vencimiento" else "Fecha")
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(uiState.date.toFullLabel())
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notas (opcional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Guardar", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onDateChange(date)
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
