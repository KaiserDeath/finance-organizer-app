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
import pe.moneyflow.core.domain.usecase.GetTransactionUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Priority
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
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
    val accountId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val allCategories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val saved: Boolean = false,
) {
    /** When the transaction is still pending, [date] is its due date rather than a paid date. */
    val isPending: Boolean get() = status == TransactionStatus.PENDING
    val categories: List<Category>
        get() = allCategories.filter {
            it.type == if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        }

    val canSave: Boolean
        get() = title.isNotBlank() && (Money.parseToMinor(amountText)?.let { it > 0 } == true)
}

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val saveTransaction: SaveTransactionUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingId: String? = savedStateHandle.toRoute<AddEditRoute>().transactionId

    private val _uiState = MutableStateFlow(AddEditUiState(isEditing = editingId != null))
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    // Preserve the original creation timestamp when editing.
    private var createdAt: Instant = Instant.now()

    init {
        viewModelScope.launch {
            val categories = categoryRepository.observeAll().first()
            val methods = paymentMethodRepository.observeAll().first()
            val accounts = accountRepository.observeAll().first()
            _uiState.update { current ->
                current.copy(
                    allCategories = categories,
                    paymentMethods = methods,
                    accounts = accounts,
                    paymentMethodId = current.paymentMethodId
                        ?: methods.firstOrNull { it.isDefault }?.id
                        ?: methods.firstOrNull()?.id,
                    accountId = current.accountId ?: accounts.firstOrNull()?.id,
                )
            }
            editingId?.let { loadTransaction(it) } ?: applyDefaultCategory()
        }
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
                amountText = Money.formatPlain(tx.amountMinor),
                type = tx.type,
                status = tx.status,
                priority = tx.priority,
                categoryId = tx.categoryId,
                paymentMethodId = tx.paymentMethodId ?: it.paymentMethodId,
                accountId = tx.accountId ?: it.accountId,
                date = tx.actualDate ?: tx.estimatedDate ?: LocalDate.now(),
                notes = tx.notes.orEmpty(),
            )
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value) }

    fun onTypeChange(type: TransactionType) = _uiState.update { state ->
        val newState = state.copy(type = type)
        // Reset the category if it no longer matches the new type.
        val stillValid = newState.categories.any { it.id == newState.categoryId }
        newState.copy(categoryId = if (stillValid) newState.categoryId else newState.categories.firstOrNull()?.id)
    }

    fun onCategorySelect(id: String) = _uiState.update { it.copy(categoryId = id) }

    fun onPaymentMethodSelect(id: String) = _uiState.update { it.copy(paymentMethodId = id) }

    fun onAccountSelect(id: String) = _uiState.update { it.copy(accountId = id) }

    fun onStatusChange(status: TransactionStatus) = _uiState.update { it.copy(status = status) }

    // Priority is intentionally not user-editable in the logging flow — it stays at its default
    // (or its loaded value when editing) and is preserved through save.
    fun onDateChange(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun save() {
        val state = _uiState.value
        val amount = Money.parseToMinor(state.amountText) ?: return
        if (!state.canSave) return

        // A pending payment stores its date as the estimate (due date) with no actual date yet;
        // a paid one records the actual date.
        val isPending = state.status == TransactionStatus.PENDING
        val transaction = Transaction(
            id = editingId ?: UUID.randomUUID().toString(),
            title = state.title.trim(),
            amountMinor = amount,
            categoryId = state.categoryId,
            paymentMethodId = state.paymentMethodId,
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
            _uiState.update { it.copy(saved = true) }
        }
    }
}
