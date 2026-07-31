package pe.moneyflow.feature.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.domain.usecase.UnmarkTransactionPaidUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.CardKind
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.component.PaymentDisplayStatus
import pe.moneyflow.core.ui.component.paymentDisplayStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * State for the read-first movement detail sheet. Holds the movement plus its resolved
 * category/method/account for display, and the full lists the in-sheet pickers choose from.
 * Every edit is committed immediately (there is no "Guardar"), so this simply mirrors the store.
 */
data class MovementDetailUiState(
    val loading: Boolean = true,
    val transaction: Transaction? = null,
    val category: Category? = null,
    val method: PaymentMethod? = null,
    val account: Account? = null,
    val categories: List<Category> = emptyList(),
    val methods: List<PaymentMethod> = emptyList(),
    val accounts: List<Account> = emptyList(),
) {
    /** Whether this month's charge reads as paid / pending / overdue, using the shared resolver. */
    val display: PaymentDisplayStatus
        get() = transaction?.let { paymentDisplayStatus(it.status, it.effectiveDate) }
            ?: PaymentDisplayStatus.PAID

    val isPending: Boolean get() = display != PaymentDisplayStatus.PAID

    /** True when the movement's method has a launchable app — drives the "Pagar con …" button. */
    val canPayWithApp: Boolean get() = !method?.deepLinkPackage.isNullOrBlank()

    /** Categories offered by the picker, matching the movement's income/expense direction. */
    val pickableCategories: List<Category>
        get() {
            val wanted = if (transaction?.type == TransactionType.INCOME) {
                CategoryType.INCOME
            } else {
                CategoryType.EXPENSE
            }
            return categories.filter { it.type == wanted }
        }
}

@HiltViewModel
class MovementDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    paymentMethodRepository: PaymentMethodRepository,
    accountRepository: AccountRepository,
    private val markPaid: MarkTransactionPaidUseCase,
    private val unmarkPaid: UnmarkTransactionPaidUseCase,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movementId: String = savedStateHandle.toRoute<MovementDetailRoute>().transactionId

    private var recentlyDeleted: Transaction? = null

    // Serializes mutations so a second edit can't read-modify-write off a stale row while the
    // previous save is still round-tripping through the store — which would silently revert fields.
    private val editMutex = Mutex()

    val uiState: StateFlow<MovementDetailUiState> =
        combine(
            transactionRepository.observeAll(),
            categoryRepository.observeAll(),
            paymentMethodRepository.observeAll(),
            accountRepository.observeAll(),
        ) { transactions, categories, methods, accounts ->
            val tx = transactions.firstOrNull { it.id == movementId }
            MovementDetailUiState(
                loading = false,
                transaction = tx,
                category = categories.firstOrNull { it.id == tx?.categoryId },
                method = methods.firstOrNull { it.id == tx?.paymentMethodId },
                account = accounts.firstOrNull { it.id == tx?.accountId },
                categories = categories,
                methods = methods,
                accounts = accounts,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovementDetailUiState(),
        )

    /**
     * Read-modify-write helper: re-reads the row *fresh from the store* (never the lagging
     * uiState snapshot) under [editMutex], applies [change], and persists it. Reading fresh +
     * serializing is what prevents a rapid second edit from clobbering the first's fields.
     */
    private fun edit(change: (Transaction) -> Transaction) {
        viewModelScope.launch {
            editMutex.withLock {
                val current = transactionRepository.getById(movementId) ?: return@withLock
                saveTransaction(change(current).copy(updatedAt = Instant.now(clock)))
            }
        }
    }

    fun setTitle(value: String) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return
        edit { it.copy(title = trimmed) }
    }

    fun setNotes(value: String) = edit { it.copy(notes = value.trim().ifBlank { null }) }

    fun setCategory(id: String?) = edit { it.copy(categoryId = id) }

    fun setDate(date: LocalDate) = edit {
        // Pending keeps the date as its due estimate with no settled date; paid records the actual.
        if (it.status == TransactionStatus.PENDING) {
            it.copy(estimatedDate = date, actualDate = null)
        } else {
            it.copy(estimatedDate = date, actualDate = date)
        }
    }

    fun setMethod(id: String?, cardKind: CardKind?) = edit { tx ->
        // Keep the account in step with the method's linked account, matching Add/Edit.
        val linkedAccountId = uiState.value.methods.firstOrNull { it.id == id }?.accountId
        tx.copy(paymentMethodId = id, cardKind = cardKind, accountId = linkedAccountId ?: tx.accountId)
    }

    fun setAccount(id: String?) = edit { it.copy(accountId = id) }

    /** One-tap settle/unsettle for the movement, reusing the shared mark-paid use cases. */
    fun togglePaid() {
        viewModelScope.launch {
            editMutex.withLock {
                val tx = transactionRepository.getById(movementId) ?: return@withLock
                if (tx.status == TransactionStatus.PAID) unmarkPaid(tx.id) else markPaid(tx.id)
            }
        }
    }

    fun duplicate() {
        viewModelScope.launch {
            editMutex.withLock {
                val tx = transactionRepository.getById(movementId) ?: return@withLock
                val now = Instant.now(clock)
                saveTransaction(tx.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now))
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            editMutex.withLock {
                val tx = transactionRepository.getById(movementId) ?: return@withLock
                recentlyDeleted = tx
                deleteTransaction(tx.id)
            }
        }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { editMutex.withLock { saveTransaction(toRestore) } }
    }
}
