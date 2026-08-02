package pe.moneyflow.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.domain.usecase.DeleteRecurringExpenseUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.ObservePaymentMethodsUseCase
import pe.moneyflow.core.domain.usecase.ObserveRecurringExpensesUseCase
import pe.moneyflow.core.domain.usecase.SaveRecurringExpenseUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.ui.component.PaymentDisplayStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * A recurring template decorated with the current-month payment state its card renders:
 * whether an occurrence has already been paid this month, and how many days remain until the
 * next scheduled run ([daysUntilDue] < 0 means it is overdue).
 */
data class RecurringItemUi(
    val template: RecurringExpense,
    val category: Category?,
    val paymentMethod: PaymentMethod?,
    val paidThisMonth: Boolean,
    val daysUntilDue: Long,
) {
    /** How the card's status pill should read, using the app-wide [PaymentDisplayStatus] vocabulary. */
    val displayStatus: PaymentDisplayStatus
        get() = when {
            paidThisMonth -> PaymentDisplayStatus.PAID
            daysUntilDue < 0L -> PaymentDisplayStatus.OVERDUE
            else -> PaymentDisplayStatus.PENDING
        }
}

data class RecurringUiState(
    val isLoading: Boolean = true,
    val items: List<RecurringItemUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencyCode: String = "PEN",
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

@HiltViewModel
class RecurringViewModel @Inject constructor(
    observeRecurring: ObserveRecurringExpensesUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observePaymentMethods: ObservePaymentMethodsUseCase,
    accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val saveRecurring: SaveRecurringExpenseUseCase,
    private val deleteRecurring: DeleteRecurringExpenseUseCase,
    private val clock: Clock,
) : ViewModel() {

    private var recentlyDeleted: RecurringExpense? = null

    // One-shot ids of the transaction to open (e.g. after "Pagar este mes" resolves an occurrence).
    private val _openTransaction = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openTransaction: SharedFlow<String> = _openTransaction.asSharedFlow()

    val uiState: StateFlow<RecurringUiState> =
        combine(
            observeRecurring(),
            observeCategories(),
            transactionRepository.observeAll(),
            observePaymentMethods(),
            accountRepository.observeAll(),
        ) { items, categories, transactions, methods, accounts ->
            val today = LocalDate.now(clock)
            val month = YearMonth.from(today)
            // Templates with a paid, generated charge landing in the current month.
            val paidTemplateIds = transactions.asSequence()
                .filter { it.status == TransactionStatus.PAID }
                .filter { it.effectiveDate?.let { date -> YearMonth.from(date) == month } == true }
                .mapNotNull { it.recurringId }
                .toSet()

            RecurringUiState(
                isLoading = false,
                items = items.map { template ->
                    RecurringItemUi(
                        template = template,
                        category = categories.firstOrNull { it.id == template.categoryId },
                        paymentMethod = methods.firstOrNull { it.id == template.paymentMethodId },
                        paidThisMonth = template.id in paidTemplateIds,
                        daysUntilDue = ChronoUnit.DAYS.between(today, template.nextRunDate),
                    )
                },
                categories = categories,
                paymentMethods = methods,
                accounts = accounts,
                currencyCode = items.firstOrNull()?.currencyCode ?: "PEN",
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecurringUiState(),
        )

    /** Creates a new template or updates an existing one (the sheet builds the full model). */
    fun save(template: RecurringExpense) {
        if (template.title.isBlank() || template.amountMinor <= 0) return
        viewModelScope.launch { saveRecurring(template.copy(title = template.title.trim())) }
    }

    /**
     * Opens *this month's* charge for [template] in the transaction editor so the user can pay it
     * with a different method just this once, without touching the template. Reuses the already
     * generated occurrence when one exists; otherwise materializes a pending one seeded from the
     * template. Emits the transaction id through [openTransaction].
     */
    fun payThisMonth(template: RecurringExpense) {
        viewModelScope.launch {
            val existing = thisMonthOccurrence(template)
            val id = existing?.id ?: newOccurrence(template).also { transactionRepository.upsert(it) }.id
            _openTransaction.emit(id)
        }
    }

    /**
     * One-tap toggle for the current month's charge: [paid] true settles it (reusing the generated
     * occurrence, or seeding one, with the template's default method); false reverts it to pending.
     */
    fun setPaidThisMonth(template: RecurringExpense, paid: Boolean) {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val now = Instant.now(clock)
            val occurrence = thisMonthOccurrence(template)
            if (paid) {
                val tx = occurrence ?: newOccurrence(template)
                transactionRepository.upsert(
                    tx.copy(status = TransactionStatus.PAID, actualDate = today, updatedAt = now),
                )
            } else {
                occurrence?.let {
                    transactionRepository.upsert(
                        it.copy(status = TransactionStatus.PENDING, actualDate = null, updatedAt = now),
                    )
                }
            }
        }
    }

    /** The transaction that already represents [template]'s charge in the current month, if any. */
    private suspend fun thisMonthOccurrence(template: RecurringExpense): Transaction? {
        val month = YearMonth.from(LocalDate.now(clock))
        return transactionRepository.observeAll().first().firstOrNull { tx ->
            tx.recurringId == template.id &&
                tx.effectiveDate?.let { YearMonth.from(it) == month } == true
        }
    }

    /** A fresh pending transaction for the current cycle, seeded from [template]. */
    private fun newOccurrence(template: RecurringExpense): Transaction {
        val now = Instant.now(clock)
        val today = LocalDate.now(clock)
        val date = if (YearMonth.from(template.nextRunDate) == YearMonth.from(today)) {
            template.nextRunDate
        } else {
            today
        }
        return Transaction(
            id = java.util.UUID.randomUUID().toString(),
            title = template.title,
            amountMinor = template.amountMinor,
            currencyCode = template.currencyCode,
            categoryId = template.categoryId,
            paymentMethodId = template.paymentMethodId,
            cardKind = template.cardKind,
            accountId = template.accountId,
            type = template.type,
            status = TransactionStatus.PENDING,
            estimatedDate = date,
            recurringId = template.id,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun toggleAutoCreate(item: RecurringExpense) {
        viewModelScope.launch { saveRecurring(item.copy(autoCreate = !item.autoCreate)) }
    }

    fun delete(id: String) {
        recentlyDeleted = uiState.value.items.firstOrNull { it.template.id == id }?.template
        viewModelScope.launch { deleteRecurring(id) }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { saveRecurring(toRestore) }
    }

    fun today(): LocalDate = LocalDate.now(clock)
}
