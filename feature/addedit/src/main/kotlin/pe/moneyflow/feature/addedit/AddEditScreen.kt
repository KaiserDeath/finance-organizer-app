package pe.moneyflow.feature.addedit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.SkeletonBlocks
import pe.moneyflow.core.designsystem.component.pressScale
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.brandSurface
import pe.moneyflow.core.designsystem.theme.noticeColors
import pe.moneyflow.core.designsystem.theme.sheetScrimColor
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.component.AmountKeypad
import pe.moneyflow.core.ui.component.AmountText
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.component.PaymentDisplayStatus
import pe.moneyflow.core.ui.component.PaymentStatusPill
import pe.moneyflow.core.ui.component.paymentDisplayStatus
import pe.moneyflow.core.ui.paymentmethod.PaymentMethodSelector
import pe.moneyflow.core.ui.paymentmethod.toCardKind
import pe.moneyflow.core.ui.paymentmethod.toNature
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.recurrence.RecurrenceEditor
import pe.moneyflow.core.ui.recurrence.recurrenceSummary
import pe.moneyflow.core.ui.util.launchPaymentApp
import pe.moneyflow.core.ui.util.toDueRelativeLabel
import pe.moneyflow.core.ui.util.toRelativeLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** How long the "Guardado" morph stays on screen before leaving — a dwell, named as such. */
private const val SaveConfirmationDwellMs = 650L

/** Cells shown inline in the category grid; the rest live behind the "Más" cell's sheet. */
private const val GridCategoryCount = 7

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var payAfterSave by remember { mutableStateOf(false) }
    // Set once the bank app has actually launched, so the resume handler below can settle the
    // charge on return. Guarded by `wasPaused` so opening the screen itself never counts as a return.
    var awaitingReturn by remember { mutableStateOf(false) }
    var wasPaused by remember { mutableStateOf(false) }

    // On save: the docked button morphs to "Guardado", dwells long enough to read, then we leave.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(SaveConfirmationDwellMs)
            val method = uiState.paymentMethods.firstOrNull { it.id == uiState.paymentMethodId }
            val launched = payAfterSave && method != null && launchPaymentApp(
                context = context,
                packageName = method.deepLinkPackage,
                appName = method.name,
                playStoreId = method.playStoreId,
                webBankingUrl = FinancePresets.webUrlFor(method.deepLinkPackage),
            )
            if (launched) {
                // Stay on screen: the bank round trip is what settles this charge, and the
                // confirmation snackbar needs a live screen under it.
                awaitingReturn = true
            } else {
                onDone()
            }
        }
    }

    // Coming back from the bank app settles the pending charge just saved — the same outcome as
    // returning from Inicio's or Próximos' pay sheet, so the gesture means one thing regardless of
    // which screen sent the user there.
    LifecycleResumeEffect(Unit) {
        if (wasPaused && awaitingReturn) {
            awaitingReturn = false
            val methodName = uiState.paymentMethods
                .firstOrNull { it.id == uiState.paymentMethodId }
                ?.name
            scope.launch {
                viewModel.settlePendingPayment()
                val result = snackbarHostState.showSnackbar(
                    message = if (methodName != null) "Pagado con $methodName" else "Pagado",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoSettlePendingPayment()
                onDone()
            }
        }
        wasPaused = false
        onPauseOrDispose { wasPaused = true }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    // The keypad is open from the start on a new entry: the amount is the first thing to fill,
    // and its own keypad beats waiting for the IME to animate in over the form.
    var showKeypad by remember(uiState.isEditing) { mutableStateOf(!uiState.isEditing) }

    // When editing something that used the notes field, reveal the disclosure so nothing is hidden.
    LaunchedEffect(uiState.isEditing, uiState.notes) {
        if (uiState.isEditing && uiState.notes.isNotBlank()) showDetails = true
    }

    // Leaving with unsaved changes asks first; a clean form exits directly.
    val guardedExit: () -> Unit = {
        if (viewModel.isDirty && !uiState.saved) showDiscardDialog = true else onDone()
    }
    BackHandler(enabled = viewModel.isDirty && !uiState.saved) { showDiscardDialog = true }

    val amountMinor = Money.parseToMinor(uiState.amountText) ?: 0L
    val loading = uiState.allCategories.isEmpty() && uiState.paymentMethods.isEmpty()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            EntryDock(
                showKeypad = showKeypad,
                amountText = uiState.amountText,
                onAmountChange = viewModel::onAmountChange,
                saved = uiState.saved,
                canSave = uiState.canSave,
                saveLabel = when {
                    uiState.isEditing -> "Guardar cambios"
                    amountMinor > 0 -> "Guardar ${Money.format(amountMinor, uiState.currencyCode)}"
                    else -> "Guardar"
                },
                onSave = viewModel::save,
            )
        },
    ) { innerPadding ->
        if (loading) {
            SkeletonBlocks(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = Spacing.lg),
                heroHeight = 148.dp,
                count = 3,
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AmountHeader(
                title = if (uiState.isEditing) "Editar movimiento" else "Nuevo movimiento",
                type = uiState.type,
                onTypeChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onTypeChange(it)
                },
                amountDisplay = "${Money.symbolFor(uiState.currencyCode)} " +
                    uiState.groupedAmountText.ifEmpty { "0" },
                hasValue = uiState.amountText.isNotEmpty(),
                onBack = guardedExit,
                onAmountClick = { showKeypad = true },
                modifier = Modifier.fillMaxWidth(),
            )

            if (!uiState.isEditing && uiState.predictions.isNotEmpty()) {
                PredictionsRow(
                    predictions = uiState.predictions,
                    currencyCode = uiState.currencyCode,
                    onApply = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.applyPrediction(it)
                    },
                )
            }

            // The movement exactly as Movimientos will render it — what you see is what gets saved.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "Así quedará en Movimientos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MovementPreviewCard(
                    uiState = uiState,
                    amountMinor = amountMinor,
                    onCategoryClick = { showCategorySheet = true },
                    onTitleClick = { titleFocus.requestFocus() },
                    onStatusToggle = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onStatusChange(
                            if (uiState.isPending) TransactionStatus.PAID else TransactionStatus.PENDING,
                        )
                    },
                    onMethodClick = { showMethodSheet = true },
                    onDateClick = { showDatePicker = true },
                )
                ConsequenceNotices(uiState)
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Descripción (opcional)") },
                placeholder = { Text(uiState.selectedCategory?.name ?: "Ej. Almuerzo, Uber, Netflix") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
                    // Typing a description hands the screen back to the IME.
                    .onFocusChanged { if (it.isFocused) showKeypad = false },
            )

            val suggestedCategory = CategorySuggester.suggest(uiState.title, uiState.categories)
            CategoryGrid(
                categories = uiState.categories,
                suggested = suggestedCategory,
                selectedId = uiState.categoryId,
                onSelect = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onCategorySelect(it.id)
                },
                onMore = { showCategorySheet = true },
            )
            if (!uiState.isEditing && suggestedCategory != null &&
                uiState.categoryId == suggestedCategory.id
            ) {
                Text(
                    text = "Categoría sugerida por la descripción.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Everything below is optional for a quick expense — tucked behind a disclosure so the
            // common path (monto → categoría → guardar) stays short.
            val chevronRotation by animateFloatAsState(
                targetValue = if (showDetails) 180f else 0f,
                label = "details-chevron",
            )
            TextButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showDetails) "Menos opciones" else "Más opciones")
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(visible = showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    // Payment state, verbose form — the preview's pill toggle is the shortcut.
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

                    // Recurrence — turns this into a repeating template (rent, subscriptions...).
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

            // A pending expense has an immediate next step. Save first so returning from the bank
            // never loses the charge the user just entered.
            val linkedMethod = uiState.paymentMethods.firstOrNull { method ->
                method.id == uiState.paymentMethodId && !method.deepLinkPackage.isNullOrBlank()
            }
            if (uiState.isPending && linkedMethod != null) {
                OutlinedButton(
                    onClick = {
                        payAfterSave = true
                        viewModel.save()
                    },
                    enabled = uiState.canSave && !uiState.saved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Pagar con ${linkedMethod.name}")
                }
                Text(
                    text = "Se guardará como pendiente y abrirá ${linkedMethod.name} al terminar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = uiState.categories,
            selectedId = uiState.categoryId,
            onSelect = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onCategorySelect(it.id)
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false },
        )
    }

    if (showMethodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMethodSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            scrimColor = MaterialTheme.sheetScrimColor,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Text("Método de pago", style = MaterialTheme.typography.titleMedium)
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
                Button(
                    onClick = { showMethodSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) { Text("Listo") }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(if (uiState.isEditing) "¿Descartar los cambios?" else "¿Descartar este movimiento?")
            },
            text = { Text("Lo que escribiste se perderá.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDone()
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando") }
            },
        )
    }
}

/**
 * The indigo brand band: back navigation, Gasto/Ingreso toggle and the amount as the headline.
 * Tapping the amount reopens the keypad. Colors come from [brandSurface], mirroring the dashboard
 * hero, so the screen carries the product's visual identity instead of a bare form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountHeader(
    title: String,
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    amountDisplay: String,
    hasValue: Boolean,
    onBack: () -> Unit,
    onAmountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = MaterialTheme.brandSurface
    Surface(
        color = brand.gradientStart,
        contentColor = brand.content,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.padding(top = Spacing.xs),
    ) {
        Column(
            modifier = Modifier.padding(
                start = Spacing.sm,
                end = Spacing.lg,
                top = Spacing.xs,
                bottom = Spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.padding(start = Spacing.sm)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf(
                        TransactionType.EXPENSE to "Gasto",
                        TransactionType.INCOME to "Ingreso",
                    )
                    types.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = type == value,
                            onClick = { onTypeChange(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = brand.container,
                                activeContentColor = brand.content,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = brand.mutedContent,
                                activeBorderColor = brand.track,
                                inactiveBorderColor = brand.track,
                            ),
                        ) { Text(label) }
                    }
                }
                Text(
                    text = amountDisplay,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasValue) brand.content else brand.mutedContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onAmountClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Monto: $amountDisplay. Toca para editar."
                        }
                        .padding(vertical = Spacing.md),
                )
            }
        }
    }
}

/** One-tap presets that fill the whole form — the user still confirms with Guardar. */
@Composable
private fun PredictionsRow(
    predictions: List<QuickShortcut>,
    currencyCode: String,
    onApply: (QuickShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Como otras veces:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            predictions.forEach { shortcut ->
                SuggestionChip(
                    onClick = { onApply(shortcut) },
                    label = {
                        Text("${shortcut.label} · ${Money.format(shortcut.amountMinor, currencyCode)}")
                    },
                )
            }
        }
    }
}

/**
 * The movement rendered with [pe.moneyflow.core.ui.component.TransactionRow]'s anatomy, built live
 * from the form state. Every zone is tappable and edits the value it displays: the avatar opens the
 * category sheet, the title focuses the description field, the pill flips pagado/pendiente, the
 * method opens its sheet and the date its picker.
 */
@Composable
private fun MovementPreviewCard(
    uiState: AddEditUiState,
    amountMinor: Long,
    onCategoryClick: () -> Unit,
    onTitleClick: () -> Unit,
    onStatusToggle: () -> Unit,
    onMethodClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = uiState.selectedCategory
    val methodName = uiState.paymentMethods
        .firstOrNull { it.id == uiState.paymentMethodId }
        ?.name
        ?: "Sin método"
    val display = paymentDisplayStatus(uiState.status, uiState.date)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onCategoryClick)
                    .semantics {
                        role = Role.Button
                        contentDescription =
                            "Categoría: ${category?.name ?: "sin categoría"}. Toca para cambiar."
                    }
                    .padding(Spacing.xs),
            ) {
                CategoryAvatar(
                    icon = iconForKey(category?.iconKey.orEmpty()),
                    accent = colorFromHex(category?.colorHex, MaterialTheme.colorScheme.primary),
                    size = 44.dp,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.sm),
            ) {
                Text(
                    text = uiState.effectiveTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(onClick = onTitleClick)
                        .semantics {
                            role = Role.Button
                            contentDescription =
                                "Descripción: ${uiState.effectiveTitle}. Toca para escribirla."
                        },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.heightIn(min = 40.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(onClick = onStatusToggle)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Estado: " +
                                    (if (uiState.isPending) "pendiente" else "pagado") +
                                    ". Toca para cambiar."
                            }
                            .padding(Spacing.xxs),
                    ) {
                        PaymentStatusPill(display, showPaid = true)
                    }
                    Text(
                        text = methodName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 120.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onMethodClick)
                            .semantics {
                                role = Role.Button
                                contentDescription =
                                    "Método de pago: $methodName. Toca para cambiar."
                            }
                            .padding(Spacing.xxs),
                    )
                    Text(
                        text = "· " + uiState.date.toRelativeLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onDateClick)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (uiState.isPending) {
                                    "Fecha de vencimiento: ${uiState.date.toRelativeLabel()}. Toca para cambiar."
                                } else {
                                    "Fecha: ${uiState.date.toRelativeLabel()}. Toca para cambiar."
                                }
                            }
                            .padding(Spacing.xxs),
                    )
                }
            }
            AmountText(
                amountMinor = amountMinor,
                currencyCode = uiState.currencyCode,
                type = uiState.type,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * Plain-language consequences of the current choices, stated before saving so nothing that
 * happens afterwards is a surprise: a pending charge announces its due phrase (and warns when it
 * will already read "Vencido"), and a recurrence announces that Guardar creates a template.
 */
@Composable
private fun ConsequenceNotices(uiState: AddEditUiState, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (uiState.isPending) {
            Text(
                text = "Se guardará como pendiente · " + uiState.date.toDueRelativeLabel(today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (paymentDisplayStatus(TransactionStatus.PENDING, uiState.date, today) ==
                PaymentDisplayStatus.OVERDUE
            ) {
                Surface(
                    color = MaterialTheme.noticeColors.dangerContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "La fecha ya pasó, así que aparecerá como “Vencido”. " +
                            "Cámbiala si el pago aún no vence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.noticeColors.onDangerContainer,
                        modifier = Modifier.padding(Spacing.sm),
                    )
                }
            }
        }
        if (uiState.isRecurring && uiState.canBeRecurring && uiState.recurrence.isValid) {
            Text(
                text = "Se creará un pago recurrente: " + recurrenceSummary(
                    daysOfMonth = uiState.recurrence.days,
                    frequency = uiState.recurrence.frequency,
                    interval = uiState.recurrence.effectiveInterval,
                ) + ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The most-used categories as colored one-tap cells, plus a "Más" cell opening the full sheet.
 * Tapping selects — it never saves. The selected (or suggested) category is always kept visible.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGrid(
    categories: List<Category>,
    suggested: Category?,
    selectedId: String?,
    onSelect: (Category) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = if (suggested != null) {
        listOf(suggested) + categories.filterNot { it.id == suggested.id }
    } else {
        categories
    }
    val visible = ordered.take(GridCategoryCount).toMutableList()
    val selected = ordered.firstOrNull { it.id == selectedId }
    if (selected != null && visible.none { it.id == selectedId }) {
        visible[visible.lastIndex] = selected
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FieldLabel("Categoría")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            maxItemsInEachRow = 4,
            modifier = Modifier.fillMaxWidth(),
        ) {
            visible.forEach { category ->
                val isSelected = category.id == selectedId
                val accent = colorFromHex(category.colorHex, MaterialTheme.colorScheme.primary)
                CategoryCell(
                    label = category.name,
                    selected = isSelected,
                    onClick = { onSelect(category) },
                    contentDescription = "Categoría ${category.name}" +
                        if (isSelected) ", seleccionada" else "",
                ) {
                    CategoryAvatar(
                        icon = iconForKey(category.iconKey),
                        accent = accent,
                        size = 44.dp,
                        modifier = if (isSelected) {
                            Modifier.border(2.dp, accent, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                }
            }
            CategoryCell(
                label = "Más",
                selected = false,
                onClick = onMore,
                contentDescription = "Ver todas las categorías",
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.MoreHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSize.md),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    avatar: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .padding(vertical = Spacing.xs),
    ) {
        avatar()
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Full category list behind the grid's "Más" cell, in the canonical sheet shape. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        scrimColor = MaterialTheme.sheetScrimColor,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("Elige una categoría", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = category.id == selectedId,
                        onClick = { onSelect(category) },
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
        }
    }
}

/**
 * The pinned footer: keypad plus the single primary action, always on screen. The button names its
 * consequence ("Guardar S/ 18.00") and morphs to "Guardado" on success.
 */
@Composable
private fun EntryDock(
    showKeypad: Boolean,
    amountText: String,
    onAmountChange: (String) -> Unit,
    saved: Boolean,
    canSave: Boolean,
    saveLabel: String,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(
            modifier = Modifier
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AnimatedVisibility(visible = showKeypad) {
                AmountKeypad(
                    value = amountText,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onSave,
                enabled = canSave && !saved,
                interactionSource = saveInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressScale(saveInteraction),
            ) {
                AnimatedContent(
                    targetState = saved,
                    transitionSpec = {
                        (fadeIn(Motion.effectsDefault()) +
                            scaleIn(Motion.spatialDefault(), initialScale = 0.8f)) togetherWith
                            fadeOut(Motion.effectsDefault())
                    },
                    label = "save-confirmation",
                ) { isSaved ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.CheckCircle
                            else Icons.Rounded.Check,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = if (isSaved) "Guardado" else saveLabel,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
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
