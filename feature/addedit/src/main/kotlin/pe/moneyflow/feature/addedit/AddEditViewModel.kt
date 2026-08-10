package pe.moneyflow.feature.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.GenerateDueRecurringUseCase
import pe.moneyflow.core.domain.usecase.GetFrequentShortcutsUseCase
import pe.moneyflow.core.domain.usecase.GetTransactionUseCase
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import pe.moneyflow.core.domain.usecase.SaveRecurringExpenseUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.domain.usecase.UnmarkTransactionPaidUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.CardKind
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Priority
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.recurrence.RecurrenceValue
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class AddEditUiState(
    val isEditing: Boolean = false,
    val title: String = "",
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val status: TransactionStatus = TransactionStatus.PAID,
    val priority: Priority = Priority.NORMAL,
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    val cardKind: CardKind? = null,
    val accountId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val recurrence: RecurrenceValue = RecurrenceValue(),
    val allCategories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val accounts: List<Account> = emptyList(),
    /** Drives the amount field's symbol prefix, which used to be hardcoded to "S/ ". */
    val currencyCode: String = "PEN",
    /** One-tap presets that *fill* the form (never save) — shown only when creating. */
    val predictions: List<QuickShortcut> = emptyList(),
    val saved: Boolean = false,
) {
    /** When the transaction is still pending, [date] is its due date rather than a paid date. */
    val isPending: Boolean get() = status == TransactionStatus.PENDING

    /** Recurrence is only offered for new entries — editing an existing movement stays one-off. */
    val canBeRecurring: Boolean get() = !isEditing

    val categories: List<Category>
        get() = allCategories.filter {
            it.type == if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        }

    val selectedCategory: Category?
        get() = allCategories.firstOrNull { it.id == categoryId }

    /**
     * The title the movement will actually carry: what the user typed, or — since the title is
     * optional — the category's name, so "Comida S/ 18" saves in two taps without naming it.
     */
    val effectiveTitle: String
        get() = title.trim().ifBlank { selectedCategory?.name ?: "Movimiento" }

    /**
     * [amountText] with thousands separators for display only ("1234567.5" → "1,234,567.5").
     * The raw buffer stays plain because the keypad edits it with dropLast.
     */
    val groupedAmountText: String
        get() {
            if (amountText.isEmpty()) return ""
            val intPart = amountText.substringBefore('.')
            val rest = amountText.drop(intPart.length)
            val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
            return grouped + rest
        }

    val canSave: Boolean
        get() = (Money.parseToMinor(amountText)?.let { it > 0 } == true) &&
            (!isRecurring || recurrence.isValid)
}

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val saveTransaction: SaveTransactionUseCase,
    private val saveRecurring: SaveRecurringExpenseUseCase,
    private val generateDueRecurring: GenerateDueRecurringUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val markTransactionPaid: MarkTransactionPaidUseCase,
    private val unmarkTransactionPaid: UnmarkTransactionPaidUseCase,
    private val getFrequentShortcuts: GetFrequentShortcutsUseCase,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingId: String? = savedStateHandle.toRoute<AddEditRoute>().transactionId

    private val _uiState = MutableStateFlow(AddEditUiState(isEditing = editingId != null))
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    // Preserve the original creation timestamp when editing.
    private var createdAt: Instant = Instant.now()

    // The transaction `save()` just wrote, held so a "Pagar con X" round trip can settle exactly
    // that row on return — the same charge, not a re-derived lookup.
    private var lastSaved: Transaction? = null

    /** Once the user picks a category by hand, description typing stops overriding it. */
    private var categoryManuallyChosen = false

    /**
     * The user-editable fields as they stood once loading finished — what "unsaved changes"
     * is measured against for the exit guard. Null until the initial load resolves, so a
     * back press during loading never counts as dirty.
     */
    private var initialSnapshot: Snapshot? = null

    private data class Snapshot(
        val title: String,
        val amountText: String,
        val type: TransactionType,
        val status: TransactionStatus,
        val categoryId: String?,
        val paymentMethodId: String?,
        val accountId: String?,
        val date: LocalDate,
        val notes: String,
        val isRecurring: Boolean,
        val recurrence: RecurrenceValue,
    )

    private fun snapshotOf(state: AddEditUiState) = Snapshot(
        title = state.title,
        amountText = state.amountText,
        type = state.type,
        status = state.status,
        categoryId = state.categoryId,
        paymentMethodId = state.paymentMethodId,
        accountId = state.accountId,
        date = state.date,
        notes = state.notes,
        isRecurring = state.isRecurring,
        recurrence = state.recurrence,
    )

    /** True when leaving now would discard something the user actually changed. */
    val isDirty: Boolean
        get() = initialSnapshot?.let { it != snapshotOf(_uiState.value) } == true

    init {
        viewModelScope.launch {
            val categories = categoryRepository.observeAll().first()
            val prefs = settingsRepository.preferences.first()
            // Only the methods the user declared having reach the selector — no chips that render
            // but can't be selected. (Archived ones are already excluded at the DAO.)
            val methods = paymentMethodRepository.observeAll().first()
                .let { all ->
                    prefs.activeMethodIds
                        ?.let { active -> all.filter { it.id in active } }
                        ?.takeIf { it.isNotEmpty() }
                        ?: all
                }
            val accounts = accountRepository.observeAll().first()
            val currency = prefs.currencyCode
            _uiState.update { current ->
                // If the default no longer exists or fell outside the user's selection, fall back
                // to the first valid method — never null, never an orphan id.
                val methodId = current.paymentMethodId?.takeIf { id -> methods.any { it.id == id } }
                    ?: methods.firstOrNull { it.isDefault }?.id
                    ?: methods.firstOrNull()?.id
                // The account follows the chosen method's linked account, so the two stay coherent.
                val method = methods.firstOrNull { it.id == methodId }
                val linkedAccountId = method?.accountId
                current.copy(
                    allCategories = categories,
                    paymentMethods = methods,
                    accounts = accounts,
                    paymentMethodId = methodId,
                    cardKind = current.cardKind ?: method?.cardKind,
                    accountId = current.accountId ?: linkedAccountId ?: accounts.firstOrNull()?.id,
                    currencyCode = currency,
                )
            }
            editingId?.let { loadTransaction(it) } ?: applyDefaultCategory()
            initialSnapshot = snapshotOf(_uiState.value)

            // Predictions fill the form; they never save. Same source as the dashboard's
            // "De un toque" row: onboarding picks first, inferred frequency as fallback.
            if (editingId == null) {
                val predictions = prefs.shortcuts
                    .ifEmpty { getFrequentShortcuts().first() }
                    .take(PREDICTION_COUNT)
                _uiState.update { it.copy(predictions = predictions) }
            }
        }
    }

    /** Fills every field the shortcut carries — the user still confirms with Guardar. */
    fun applyPrediction(shortcut: QuickShortcut) = _uiState.update { state ->
        categoryManuallyChosen = true
        val method = state.paymentMethods.firstOrNull { it.id == shortcut.paymentMethodId }
        val categoryId = shortcut.categoryId
            ?.takeIf { id -> state.categories.any { it.id == id } }
            ?: state.categoryId
        state.copy(
            title = shortcut.label,
            amountText = plainAmountText(shortcut.amountMinor),
            categoryId = categoryId,
            paymentMethodId = method?.id ?: state.paymentMethodId,
            cardKind = method?.cardKind ?: state.cardKind,
            accountId = method?.accountId ?: state.accountId,
        )
    }

    private fun applyDefaultCategory() {
        _uiState.update { state ->
            if (state.categoryId != null) return@update state
            state.copy(categoryId = state.categories.firstOrNull()?.id)
        }
    }

    private suspend fun loadTransaction(id: String) {
        val tx = getTransaction(id) ?: return
        createdAt = tx.createdAt
        _uiState.update {
            it.copy(
                title = tx.title,
                // Plain buffer, no separators: the keypad edits with dropLast, so a loaded
                // "1,234.56" would strand a comma the user can only delete digit by digit.
                amountText = plainAmountText(tx.amountMinor),
                type = tx.type,
                status = tx.status,
                priority = tx.priority,
                categoryId = tx.categoryId,
                paymentMethodId = tx.paymentMethodId ?: it.paymentMethodId,
                cardKind = tx.cardKind,
                accountId = tx.accountId ?: it.accountId,
                date = tx.actualDate ?: tx.estimatedDate ?: LocalDate.now(),
                notes = tx.notes.orEmpty(),
            )
        }
    }

    fun onTitleChange(value: String) = _uiState.update { state ->
        val updated = state.copy(title = value)
        // "almuerzo" already implies Comida; suggest it, but never over a manual choice or
        // while editing an existing movement.
        if (categoryManuallyChosen || state.isEditing || updated.type != TransactionType.EXPENSE) {
            updated
        } else {
            val suggestion = CategorySuggester.suggest(value, updated.allCategories)
            updated.copy(categoryId = suggestion?.id ?: updated.categoryId)
        }
    }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value) }

    fun onTypeChange(type: TransactionType) = _uiState.update { state ->
        // Switching type discards the previous category choice, so suggestions apply again.
        categoryManuallyChosen = false
        val newState = state.copy(type = type)
        // Reset the category if it no longer matches the new type.
        val stillValid = newState.categories.any { it.id == newState.categoryId }
        newState.copy(categoryId = if (stillValid) newState.categoryId else newState.categories.firstOrNull()?.id)
    }

    fun onCategorySelect(id: String) {
        categoryManuallyChosen = true
        _uiState.update { it.copy(categoryId = id) }
    }

    fun onPaymentMethodSelect(id: String?, cardKind: CardKind?) = _uiState.update { state ->
        // Pull the transaction onto the method's linked account so the pair can't drift; a method
        // with no linked account leaves the current account untouched for the user to set.
        val linkedAccountId = state.paymentMethods.firstOrNull { it.id == id }?.accountId
        state.copy(
            paymentMethodId = id,
            cardKind = cardKind,
            accountId = linkedAccountId ?: state.accountId,
        )
    }

    fun onAccountSelect(id: String) = _uiState.update { it.copy(accountId = id) }

    fun onStatusChange(status: TransactionStatus) = _uiState.update { it.copy(status = status) }

    // Priority is intentionally not user-editable in the logging flow — it stays at its default
    // (or its loaded value when editing) and is preserved through save.
    fun onDateChange(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onRecurringChange(recurring: Boolean) = _uiState.update { it.copy(isRecurring = recurring) }

    fun onRecurrenceChange(value: RecurrenceValue) = _uiState.update { it.copy(recurrence = value) }

    fun save() {
        val state = _uiState.value
        val amount = Money.parseToMinor(state.amountText) ?: return
        if (!state.canSave) return

        // A recurring entry saves a template instead of a one-off transaction; the background worker
        // then materializes its due occurrences as PENDING transactions.
        if (state.isRecurring && state.canBeRecurring) {
            saveRecurringTemplate(state, amount)
            return
        }

        // A pending payment stores its date as the estimate (due date) with no actual date yet;
        // a paid one records the actual date.
        val isPending = state.status == TransactionStatus.PENDING
        val transaction = Transaction(
            id = editingId ?: UUID.randomUUID().toString(),
            title = state.effectiveTitle,
            amountMinor = amount,
            categoryId = state.categoryId,
            paymentMethodId = state.paymentMethodId,
            cardKind = state.cardKind,
            accountId = state.accountId,
            type = state.type,
            status = state.status,
            priority = state.priority,
            actualDate = if (isPending) null else state.date,
            estimatedDate = state.date,
            notes = state.notes.trim().ifBlank { null },
            createdAt = createdAt,
            updatedAt = Instant.now(),
        )

        viewModelScope.launch {
            saveTransaction(transaction)
            lastSaved = transaction
            _uiState.update { it.copy(saved = true) }
        }
    }

    /**
     * Settles the pending charge [save] just wrote, as paid with its own method — the launch-and-
     * return counterpart to "Pagar con X" in [pe.moneyflow.feature.addedit.AddEditScreen]. Mirrors
     * what Inicio and Próximos do when the bank app is closed, so the gesture means the same thing
     * regardless of which screen sent the user there instead of leaving the charge pending only here.
     */
    // `suspend`, not a fire-and-forget `viewModelScope.launch`: the caller navigates away right
    // after calling these (`onDone()`, immediately following `undoSettlePendingPayment()` with no
    // suspension in between). A fire-and-forget child coroutine here raced the nav pop that clears
    // this ViewModel's scope — the "Deshacer" write could lose that race and silently never happen.
    // Being `suspend` makes the caller's own coroutine wait for the write before moving on.
    suspend fun settlePendingPayment() {
        val tx = lastSaved ?: return
        markTransactionPaid(tx.id, tx.paymentMethodId)
    }

    /** Reverts [settlePendingPayment] (the snackbar's "Deshacer"). */
    suspend fun undoSettlePendingPayment() {
        val tx = lastSaved ?: return
        unmarkTransactionPaid(tx.id)
    }

    private fun saveRecurringTemplate(state: AddEditUiState, amount: Long) {
        val days = state.recurrence.days
        val template = RecurringExpense(
            id = UUID.randomUUID().toString(),
            title = state.effectiveTitle,
            amountMinor = amount,
            categoryId = state.categoryId,
            paymentMethodId = state.paymentMethodId,
            cardKind = state.cardKind,
            accountId = state.accountId,
            type = state.type,
            frequency = state.recurrence.frequency,
            interval = state.recurrence.effectiveInterval,
            daysOfMonth = days,
            // The chosen date seeds the schedule; for day-of-month mode it snaps to the first hit.
            nextRunDate = RecurringExpense.firstRunOnOrAfter(state.date, days),
        )
        viewModelScope.launch {
            saveRecurring(template)
            // Materialize any now-due occurrence (e.g. today's) right away, so "Se repite" produces a
            // visible movement instead of only a template tucked away under Pagos recurrentes.
            generateDueRecurring()
            _uiState.update { it.copy(saved = true) }
        }
    }

    private companion object {
        const val PREDICTION_COUNT = 3

        /**
         * Minor units as a keypad buffer: plain digits, dot separator, no grouping, and whole
         * amounts without the ".00" tail ("1800" → "18", "1850" → "18.50").
         */
        fun plainAmountText(amountMinor: Long): String =
            Money.formatPlain(amountMinor).replace(",", "").removeSuffix(".00")
    }
}
