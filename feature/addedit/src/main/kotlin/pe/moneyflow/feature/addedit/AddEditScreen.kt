package pe.moneyflow.feature.addedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.component.pressScale
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.component.PaymentDisplayStatus
import pe.moneyflow.core.ui.component.PaymentStatusPill
import pe.moneyflow.core.ui.component.paymentDisplayStatus
import pe.moneyflow.core.ui.component.AmountKeypad
import pe.moneyflow.core.ui.paymentmethod.PaymentMethodSelector
import pe.moneyflow.core.ui.paymentmethod.toCardKind
import pe.moneyflow.core.ui.paymentmethod.toNature
import pe.moneyflow.core.ui.recurrence.RecurrenceEditor
import pe.moneyflow.core.ui.util.toDueRelativeLabel
import pe.moneyflow.core.ui.util.toRelativeLabel
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
    val haptics = LocalHapticFeedback.current

    // On save: the button itself confirms, then we leave.
    //
    // This used to fire a snackbar and then `delay(500)` before navigating — two problems. The dwell
    // was an arbitrary sleep unrelated to anything on screen, and the snackbar was shown on a screen
    // that was about to be destroyed, so the confirmation the user was meant to read left with it.
    //
    // Now the save button morphs to a "Guardado" state and we await *that animation* before calling
    // [onDone]. The dwell is the animation's own duration, so the timing is a consequence of the
    // motion rather than a magic number, and the confirmation is visible where the user just tapped.
    val confirmation = remember { Animatable(0f) }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            confirmation.animateTo(1f, animationSpec = Motion.spatialSlow())
            onDone()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    // The keypad is open from the start on a new entry: the amount is the first thing to fill,
    // and its own keypad beats waiting for the IME to animate in over the form.
    val isEditing by rememberUpdatedState(uiState.isEditing)
    var showKeypad by remember { mutableStateOf(!isEditing) }

    // When editing something that used the advanced fields, reveal them so nothing is hidden.
    LaunchedEffect(uiState.isEditing, uiState.status, uiState.notes, uiState.date) {
        if (uiState.isEditing &&
            (uiState.status == TransactionStatus.PENDING ||
                uiState.notes.isNotBlank() ||
                uiState.date != LocalDate.now())
        ) {
            showDetails = true
        }
    }

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
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onTypeChange(type)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    ) { Text(label) }
                }
            }

            // Amount — a display, not an IME field: the in-app keypad below drives it.
            AmountDisplay(
                amountText = uiState.amountText,
                currencyCode = uiState.currencyCode,
                onClick = { showKeypad = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (showKeypad) {
                AmountKeypad(
                    value = uiState.amountText,
                    onValueChange = viewModel::onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Descripción") },
                placeholder = { Text("Ej. Almuerzo, Uber, Netflix") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    // Typing a description hands the screen back to the IME.
                    .onFocusChanged { if (it.isFocused) showKeypad = false },
            )

            QuickEntrySummary(
                paymentMethod = uiState.paymentMethods
                    .firstOrNull { it.id == uiState.paymentMethodId }
                    ?.name
                    ?: "Sin método",
                date = uiState.date.toRelativeLabel(),
                onOpenDetails = { showDetails = true },
                modifier = Modifier.fillMaxWidth(),
            )

            val suggestedCategory = CategorySuggester.suggest(uiState.title, uiState.categories)
            if (!uiState.isEditing && suggestedCategory != null &&
                uiState.categoryId == suggestedCategory.id
            ) {
                Text(
                    text = "Categoría sugerida: ${suggestedCategory.name}. Puedes cambiarla abajo.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Category
            FieldLabel("Categoría")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                uiState.categories.forEach { category ->
                    FilterChip(
                        selected = uiState.categoryId == category.id,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onCategorySelect(category.id)
                        },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconForKey(category.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.chip),
                            )
                        },
                    )
                }
            }

            // Payment method — pick Efectivo / Débito / Crédito first, then the matching method.
            FieldLabel("Método de pago")
            PaymentMethodSelector(
                methods = uiState.paymentMethods,
                selectedId = uiState.paymentMethodId,
                selectedNature = uiState.cardKind.toNature(),
                onSelect = { id, nature ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onPaymentMethodSelect(id, if (id == null) null else nature.toCardKind())
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Recurrence — turns this into a repeating template (rent, subscriptions, salary...).
            // Offered only when creating; editing an existing movement stays one-off.
            if (uiState.canBeRecurring) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FieldLabel("Se repite")
                    Switch(
                        checked = uiState.isRecurring,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onRecurringChange(it)
                        },
                    )
                }
                AnimatedVisibility(visible = uiState.isRecurring) {
                    RecurrenceEditor(
                        value = uiState.recurrence,
                        onValueChange = viewModel::onRecurrenceChange,
                    )
                }
            }

            // Everything below is optional for a quick expense — tuck it behind a disclosure so the
            // common path (amount → description → category → method) stays short.
            val chevronRotation by animateFloatAsState(
                targetValue = if (showDetails) 180f else 0f,
                label = "details-chevron",
            )
            TextButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showDetails) "Menos detalles" else "Más detalles")
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(visible = showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    // Payment state: paid now vs still owed (an upcoming payment). Framed as a
                    // question so the choice reads as "have you paid?" rather than an abstract state.
                    FieldLabel("¿Ya pagaste esto?")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val statuses = listOf(
                            TransactionStatus.PAID to "Ya lo pagué",
                            TransactionStatus.PENDING to "Aún no",
                        )
                        statuses.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = uiState.status == value,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.onStatusChange(value)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
                            ) { Text(label) }
                        }
                    }

                    // Account (drives account balances & net worth)
                    if (uiState.accounts.isNotEmpty()) {
                        FieldLabel("Cuenta")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            uiState.accounts.forEach { account ->
                                FilterChip(
                                    selected = uiState.accountId == account.id,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.onAccountSelect(account.id)
                                    },
                                    label = { Text(account.name) },
                                )
                            }
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

                    // For a pending charge, foreshadow exactly how the list will render it — so a
                    // past due date reading as red "Vencido" is a visible consequence, not a surprise.
                    if (uiState.isPending) {
                        PendingStatusPreview(date = uiState.date)
                    }

                    // Notes
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text("Notas (opcional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave && !uiState.saved,
                interactionSource = saveInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressScale(saveInteraction),
            ) {
                AnimatedContent(
                    targetState = uiState.saved,
                    transitionSpec = {
                        (fadeIn(Motion.effectsDefault()) +
                            scaleIn(Motion.spatialDefault(), initialScale = 0.8f)) togetherWith
                            fadeOut(Motion.effectsDefault())
                    },
                    label = "save-confirmation",
                ) { saved ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (saved) Icons.Rounded.CheckCircle
                            else Icons.Rounded.Check,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = if (saved) "Guardado" else "Guardar",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
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

/** The amount as a large read-only figure; tapping it brings the keypad back. */
@Composable
private fun AmountDisplay(
    amountText: String,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasValue = amountText.isNotEmpty()
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Text(
            text = "Monto",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${Money.symbolFor(currencyCode)} ${if (hasValue) amountText else "0"}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (hasValue) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * Keeps the two choices users most often need to confirm visible while the IME covers the form.
 * Tapping the summary opens the existing advanced section, so it is a shortcut rather than a second
 * source of truth for payment method or date.
 */
@Composable
private fun QuickEntrySummary(
    paymentMethod: String,
    date: String,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onOpenDetails),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Método de pago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = paymentMethod, style = MaterialTheme.typography.labelLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Fecha",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = date, style = MaterialTheme.typography.labelLarge)
            }
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

/**
 * Mirrors how [pe.moneyflow.core.ui.component.TransactionRow] will render this pending charge:
 * the same status pill plus a plain-language due phrase. When the chosen date is already past,
 * it shows the red "Vencido" state up front, with a nudge to adjust the date.
 */
@Composable
private fun PendingStatusPreview(date: LocalDate) {
    val today = LocalDate.now()
    val display = paymentDisplayStatus(TransactionStatus.PENDING, date, today)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Así se verá en tu lista",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PaymentStatusPill(display)
            Text(
                text = date.toDueRelativeLabel(today).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (display == PaymentDisplayStatus.OVERDUE) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "La fecha ya pasó, así que aparecerá como “Vencido”. " +
                        "Cámbiala si el pago aún no vence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        }
    }
}
