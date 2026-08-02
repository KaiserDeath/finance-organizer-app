package pe.moneyflow.feature.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.CardKind
import pe.moneyflow.core.ui.component.AmountText
import pe.moneyflow.core.ui.component.PaymentDisplayStatus
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.util.launchPaymentApp
import pe.moneyflow.core.ui.util.toDueRelativeLabel
import pe.moneyflow.core.ui.util.toShortLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class SheetMode { DETAIL, PICK_CATEGORY, PICK_METHOD, PICK_ACCOUNT }

/**
 * Read-first detail for a single movement, shown as a bottom sheet. At rest it reads as content
 * (amount, title, a row of icon-led attribute chips); each chip turns editable only on tap, and
 * every change autosaves — there is no form to submit. A pending charge whose method has an app
 * gets a "Pagar con …" button that opens the bank/wallet app and, on return, offers to settle it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovementDetailScreen(
    onEditAll: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var mode by remember { mutableStateOf(SheetMode.DETAIL) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPaidPrompt by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        val tx = uiState.transaction
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (tx == null) {
                Text(
                    text = if (uiState.loading) "Cargando…" else "Este movimiento ya no existe.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xl),
                )
                return@Column
            }

            when (mode) {
                SheetMode.DETAIL -> DetailContent(
                    state = uiState,
                    onTogglePaid = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.togglePaid()
                    },
                    onTitleCommit = viewModel::setTitle,
                    onNotesCommit = viewModel::setNotes,
                    onPickCategory = { mode = SheetMode.PICK_CATEGORY },
                    onPickMethod = { mode = SheetMode.PICK_METHOD },
                    onPickAccount = { mode = SheetMode.PICK_ACCOUNT },
                    onPickDate = { showDatePicker = true },
                    onPay = {
                        val method = uiState.method ?: return@DetailContent
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val launched = launchPaymentApp(
                            context = context,
                            packageName = method.deepLinkPackage,
                            appName = method.name,
                            playStoreId = method.playStoreId,
                            webBankingUrl = FinancePresets.webUrlFor(method.deepLinkPackage),
                        )
                        // If we sent the user off to pay, ask whether it went through on return.
                        if (launched) showPaidPrompt = true
                    },
                    onMarkPaid = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.togglePaid()
                    },
                    onDuplicate = { viewModel.duplicate(); onDone() },
                    onDelete = { showDeleteConfirm = true },
                    onEditAll = { onEditAll(tx.id) },
                )

                SheetMode.PICK_CATEGORY -> CategoryPicker(
                    state = uiState,
                    onBack = { mode = SheetMode.DETAIL },
                    onSelect = { id -> viewModel.setCategory(id); mode = SheetMode.DETAIL },
                )

                SheetMode.PICK_METHOD -> MethodPicker(
                    state = uiState,
                    onBack = { mode = SheetMode.DETAIL },
                    onSelect = { id, cardKind -> viewModel.setMethod(id, cardKind); mode = SheetMode.DETAIL },
                )

                SheetMode.PICK_ACCOUNT -> AccountPicker(
                    state = uiState,
                    onBack = { mode = SheetMode.DETAIL },
                    onSelect = { id -> viewModel.setAccount(id); mode = SheetMode.DETAIL },
                )
            }
        }
    }

    if (showDatePicker) {
        val initial = (uiState.transaction?.effectiveDate ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Se quitará de tus movimientos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                    onDone()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } },
        )
    }

    // Shown after returning from the bank/wallet app, to close the loop in one tap.
    if (showPaidPrompt && uiState.isPending) {
        val methodName = uiState.method?.name ?: "tu método"
        AlertDialog(
            onDismissRequest = { showPaidPrompt = false },
            title = { Text("¿Terminaste el pago?") },
            text = { Text("Si ya pagaste con $methodName, lo marcamos como pagado.") },
            confirmButton = {
                TextButton(onClick = {
                    showPaidPrompt = false
                    viewModel.togglePaid()
                }) { Text("Sí, marcar pagado") }
            },
            dismissButton = { TextButton(onClick = { showPaidPrompt = false }) { Text("Todavía no") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    state: MovementDetailUiState,
    onTogglePaid: () -> Unit,
    onTitleCommit: (String) -> Unit,
    onNotesCommit: (String) -> Unit,
    onPickCategory: () -> Unit,
    onPickMethod: () -> Unit,
    onPickAccount: () -> Unit,
    onPickDate: () -> Unit,
    onPay: () -> Unit,
    onMarkPaid: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onEditAll: () -> Unit,
) {
    val tx = state.transaction ?: return

    // Hero: signed, colored amount (mutes when not yet paid), then an inline-editable title.
    AmountText(
        amountMinor = tx.amountMinor,
        currencyCode = tx.currencyCode,
        type = tx.type,
        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        color = if (state.isPending) MaterialTheme.colorScheme.onSurfaceVariant else null,
    )
    InlineEditableText(
        value = tx.title,
        placeholder = "Sin descripción",
        textStyle = MaterialTheme.typography.headlineSmall,
        onCommit = onTitleCommit,
    )

    // Attribute chips — the icon carries the meaning, no field-name labels.
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        StatusChip(display = state.display, onClick = onTogglePaid)
        PickChip(
            icon = iconForKey(state.category?.iconKey ?: "category"),
            tint = colorFromHex(state.category?.colorHex, MaterialTheme.colorScheme.primary),
            label = state.category?.name ?: "Sin categoría",
            onClick = onPickCategory,
        )
        PickChip(
            icon = Icons.Rounded.CalendarToday,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            label = tx.effectiveDate?.toShortLabel() ?: "Sin fecha",
            onClick = onPickDate,
        )
        PickChip(
            icon = iconForKey(state.method?.iconKey ?: "payments"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            label = state.method?.name ?: "Sin método",
            onClick = onPickMethod,
        )
        if (state.accounts.isNotEmpty()) {
            PickChip(
                icon = iconForKey(state.account?.iconKey ?: "account_balance"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                label = state.account?.name ?: "Sin cuenta",
                onClick = onPickAccount,
            )
        }
    }

    // A pending charge's due phrasing lives here so the chips stay uncluttered.
    if (state.isPending) {
        Text(
            text = tx.effectiveDate?.toDueRelativeLabel()?.replaceFirstChar { it.uppercase() }
                ?: "Sin fecha de vencimiento",
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.display == PaymentDisplayStatus.OVERDUE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    InlineEditableText(
        value = tx.notes.orEmpty(),
        placeholder = "Agregar una nota…",
        textStyle = MaterialTheme.typography.bodyLarge,
        onCommit = onNotesCommit,
        singleLine = false,
        modifier = Modifier.padding(top = Spacing.xs),
    )

    // Primary action for a pending charge: open the bank/wallet app, or just settle it (cash).
    if (state.isPending) {
        Button(
            onClick = if (state.canPayWithApp) onPay else onMarkPaid,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = Spacing.sm),
        ) {
            if (state.canPayWithApp) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Pagar con ${state.method?.name}")
            } else {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Marcar pagado")
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Duplicar")
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Eliminar")
        }
    }

    TextButton(onClick = onEditAll, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text("Editar todo")
    }
}

/** Tonal status pill that is itself the paid/pending toggle. */
@Composable
private fun StatusChip(display: PaymentDisplayStatus, onClick: () -> Unit) {
    val (label, container, content) = when (display) {
        PaymentDisplayStatus.PAID -> Triple(
            "Pagado",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        PaymentDisplayStatus.PENDING -> Triple(
            "Pendiente",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        PaymentDisplayStatus.OVERDUE -> Triple(
            "Vencido",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (display == PaymentDisplayStatus.PAID) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

/** A tappable attribute chip: leading icon, current value, and a dropdown caret. */
@Composable
private fun PickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/** Reads as plain text; tapping turns it into a field that autosaves when it loses focus. */
@Composable
private fun InlineEditableText(
    value: String,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    var editing by remember(value) { mutableStateOf(false) }
    var draft by remember(value) { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }

    if (editing) {
        // Only treat a focus *loss* as a commit once focus has actually landed — otherwise the
        // initial unfocused pass would commit-and-exit before the user can type.
        var hasFocused by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = singleLine,
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
            keyboardActions = KeyboardActions(onDone = { onCommit(draft); editing = false }),
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        hasFocused = true
                    } else if (hasFocused) {
                        onCommit(draft)
                        editing = false
                    }
                },
        )
    } else {
        val isEmpty = value.isBlank()
        Text(
            text = if (isEmpty) placeholder else value,
            style = textStyle,
            color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = modifier
                .fillMaxWidth()
                .clickable { draft = value; editing = true }
                .padding(vertical = Spacing.xs),
        )
    }
}

@Composable
private fun PickerHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CategoryPicker(
    state: MovementDetailUiState,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val all = state.pickableCategories
    // Type-to-filter earns its keep here (long list); short lists below skip the search field.
    val filtered = if (query.isBlank()) all else all.filter { it.name.contains(query.trim(), ignoreCase = true) }

    PickerHeader("Categoría", onBack)
    if (all.size > 8) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Escribe para filtrar…") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    OptionRow(
        icon = iconForKey("category"),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        label = "Sin categoría",
        selected = state.transaction?.categoryId == null,
        onClick = { onSelect(null) },
    )
    filtered.forEach { category ->
        OptionRow(
            icon = iconForKey(category.iconKey),
            tint = colorFromHex(category.colorHex, MaterialTheme.colorScheme.primary),
            label = category.name,
            selected = state.transaction?.categoryId == category.id,
            onClick = { onSelect(category.id) },
        )
    }
    if (filtered.isEmpty()) {
        Text(
            "Sin coincidencias",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Composable
private fun MethodPicker(
    state: MovementDetailUiState,
    onBack: () -> Unit,
    onSelect: (String?, CardKind?) -> Unit,
) {
    PickerHeader("Método de pago", onBack)
    OptionRow(
        icon = iconForKey("payments"),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        label = "Sin método",
        selected = state.transaction?.paymentMethodId == null,
        onClick = { onSelect(null, null) },
    )
    state.methods.forEach { method ->
        OptionRow(
            icon = iconForKey(method.iconKey),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            label = method.name,
            selected = state.transaction?.paymentMethodId == method.id,
            onClick = { onSelect(method.id, method.cardKind) },
        )
    }
}

@Composable
private fun AccountPicker(
    state: MovementDetailUiState,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    PickerHeader("Cuenta", onBack)
    OptionRow(
        icon = iconForKey("account_balance"),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        label = "Sin cuenta",
        selected = state.transaction?.accountId == null,
        onClick = { onSelect(null) },
    )
    state.accounts.forEach { account ->
        OptionRow(
            icon = iconForKey(account.iconKey),
            tint = colorFromHex(account.colorHex, MaterialTheme.colorScheme.primary),
            label = account.name,
            selected = state.transaction?.accountId == account.id,
            onClick = { onSelect(account.id) },
        )
    }
}

@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(Spacing.md))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
