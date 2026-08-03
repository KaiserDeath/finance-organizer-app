package pe.moneyflow.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.BudgetRepository
import pe.moneyflow.core.domain.repository.ExchangeRateRepository
import pe.moneyflow.core.domain.repository.SavingsGoalRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.SmartInsights
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.ExchangeRate
import pe.moneyflow.core.model.SavingsGoal
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.UserPreferences
import java.time.LocalDate

/**
 * In-memory repository fakes shared by every ViewModel test.
 *
 * They live here rather than beside each test because four feature modules need the same six fakes,
 * and a repository interface changing should break one file rather than four copies that have
 * quietly drifted apart.
 *
 * [FakeTransactionRepository] is genuinely stateful — writes are observable — because the behaviour
 * most worth testing is what a ViewModel *persists*. The rest are read-only unless a test needs
 * otherwise; a fake that stores what nobody reads is just noise.
 */
class FakeTransactionRepository(seed: List<Transaction> = emptyList()) : TransactionRepository {
    private val store = MutableStateFlow(seed.associateBy { it.id })
    val state = store.asStateFlow()

    fun all(): List<Transaction> = store.value.values.toList()
    operator fun get(id: String): Transaction? = store.value[id]

    override fun observeAll(): Flow<List<Transaction>> = flowOf(all())

    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        flowOf(all().filter { tx -> tx.effectiveDate?.let { it in start..end } == true })

    override suspend fun getById(id: String): Transaction? = get(id)

    override suspend fun upsert(transaction: Transaction) {
        store.value = store.value + (transaction.id to transaction)
    }

    override suspend fun delete(id: String) {
        store.value = store.value - id
    }
}

class FakeCategoryRepository(private val items: List<Category> = emptyList()) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = flowOf(items)
    override suspend fun getById(id: String): Category? = items.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category) = Unit
    override suspend fun delete(id: String) = Unit
}

/** Stateful for the same reason as [FakeTransactionRepository]: deleting a budget has an undo. */
class FakeBudgetRepository(seed: List<Budget> = emptyList()) : BudgetRepository {
    private val store = MutableStateFlow(seed.associateBy { it.id })

    fun all(): List<Budget> = store.value.values.toList()

    // Live rather than a `flowOf` snapshot: undo is only observable if a write re-emits, and a
    // one-shot flow completes before the delete happens, so the state under test never changes.
    override fun observeAll(): Flow<List<Budget>> = store.map { it.values.toList() }
    override suspend fun getById(id: String): Budget? = store.value[id]

    override suspend fun upsert(budget: Budget) {
        store.value = store.value + (budget.id to budget)
    }

    override suspend fun delete(id: String) {
        store.value = store.value - id
    }
}

/**
 * Stateful, because "which method is the default" is a fact about the whole set that only a write
 * can change — and a fake whose `upsert` returned `Unit` could not tell a use case that sets the
 * default correctly from one that sets two.
 *
 * Preserves insertion order: the thing being guarded against is a consumer falling back to
 * `firstOrNull { it.isDefault }` and answering by position, so the order has to be stable enough
 * for a test to catch that.
 */
class FakePaymentMethodRepository(
    seed: List<PaymentMethod> = emptyList(),
) : PaymentMethodRepository {
    private val store = MutableStateFlow(seed)

    fun all(): List<PaymentMethod> = store.value

    override fun observeAll(): Flow<List<PaymentMethod>> = store.asStateFlow()
    override suspend fun getById(id: String): PaymentMethod? = store.value.firstOrNull { it.id == id }

    override suspend fun upsert(paymentMethod: PaymentMethod) {
        val existing = store.value.indexOfFirst { it.id == paymentMethod.id }
        store.value = if (existing >= 0) {
            store.value.toMutableList().also { it[existing] = paymentMethod }
        } else {
            store.value + paymentMethod
        }
    }

    override suspend fun delete(id: String) {
        store.value = store.value.filterNot { it.id == id }
    }
}

class FakeRecurringExpenseRepository(
    private val items: List<RecurringExpense> = emptyList(),
) : RecurringExpenseRepository {
    override fun observeAll(): Flow<List<RecurringExpense>> = flowOf(items)
    override suspend fun getDue(date: LocalDate): List<RecurringExpense> = emptyList()
    override suspend fun getById(id: String): RecurringExpense? = items.firstOrNull { it.id == id }
    override suspend fun upsert(recurring: RecurringExpense) = Unit
    override suspend fun delete(id: String) = Unit
}

class FakeSmartInsights(private val items: List<Insight> = emptyList()) : SmartInsights {
    override fun observe(): Flow<List<Insight>> = flowOf(items)
}

/**
 * Preferences as a store, seeded from the constructor.
 *
 * The constructor arguments are still how a test says "given a monthly budget of X" — that is what
 * most callers need and none of them changed. But the setters are real, because they stopped being
 * incidental: the monthly budget can now be edited from Presupuestos, so "did the ViewModel actually
 * persist it, and does undo put the old value back" is the behaviour under test. A no-op setter
 * would have let both pass while doing nothing.
 *
 * [current] is the direct read, matching how [FakeTransactionRepository] exposes its rows — asserting
 * on the store is steadier than re-collecting a `WhileSubscribed` state flow to observe a write.
 */
class FakeSettingsRepository(
    monthlyBudgetMinor: Long? = null,
    shortcuts: List<QuickShortcut> = emptyList(),
    currencyCode: String = "PEN",
    activeMethodIds: Set<String>? = null,
) : SettingsRepository {
    private val store = MutableStateFlow(
        UserPreferences(
            currencyCode = currencyCode,
            monthlyBudgetMinor = monthlyBudgetMinor,
            shortcuts = shortcuts,
            activeMethodIds = activeMethodIds,
        ),
    )

    val current: UserPreferences get() = store.value

    override val preferences: Flow<UserPreferences> = store

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.value = store.value.copy(themeMode = mode)
    }

    override suspend fun setCurrency(code: String) {
        store.value = store.value.copy(currencyCode = code)
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        store.value = store.value.copy(onboardingComplete = complete)
    }

    override suspend fun setPinHash(hash: String?) = Unit
    override suspend fun setBiometricEnabled(enabled: Boolean) = Unit

    override suspend fun setMonthlyBudget(minor: Long?) {
        store.value = store.value.copy(monthlyBudgetMinor = minor)
    }

    override suspend fun setActiveMethodIds(ids: Set<String>?) {
        store.value = store.value.copy(activeMethodIds = ids)
    }

    override suspend fun setShortcuts(shortcuts: List<QuickShortcut>) {
        store.value = store.value.copy(shortcuts = shortcuts)
    }

    // Backed by the store rather than a no-op: the dashboard's toggle reads the current value
    // before flipping it, so a fake that swallowed the write would make that look like it worked.
    override suspend fun setAmountsHidden(hidden: Boolean) {
        store.value = store.value.copy(amountsHidden = hidden)
    }
}

class FakeAccountRepository(private val items: List<Account> = emptyList()) : AccountRepository {
    override fun observeAll(): Flow<List<Account>> = flowOf(items)
    override suspend fun getById(id: String): Account? = items.firstOrNull { it.id == id }
    override suspend fun upsert(account: Account) = Unit
}

class FakeSavingsGoalRepository(
    private val items: List<SavingsGoal> = emptyList(),
) : SavingsGoalRepository {
    override fun observeAll(): Flow<List<SavingsGoal>> = flowOf(items)
    override suspend fun getById(id: String): SavingsGoal? = items.firstOrNull { it.id == id }
    override suspend fun upsert(goal: SavingsGoal) = Unit
    override suspend fun delete(id: String) = Unit
}

/** Empty by default: single-currency is the ordinary case, and conversion is not what these test. */
class FakeExchangeRateRepository(
    private val items: List<ExchangeRate> = emptyList(),
) : ExchangeRateRepository {
    override fun observeAll(): Flow<List<ExchangeRate>> = flowOf(items)
    override suspend fun upsert(rate: ExchangeRate) = Unit
    override suspend fun delete(id: String) = Unit
}
