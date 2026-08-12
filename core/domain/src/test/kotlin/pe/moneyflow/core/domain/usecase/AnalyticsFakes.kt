package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.UserPreferences
import java.time.LocalDate

/** Shared in-memory fakes for the analytics/report/export use-case tests. */

internal class FakeTxRepo(private val items: List<Transaction>) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> = flowOf(items)
    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        flowOf(items.filter { tx -> tx.effectiveDate?.let { it in start..end } == true })
    override suspend fun getById(id: String): Transaction? = items.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction) = Unit
    override suspend fun delete(id: String) = Unit
}

internal class FakeCatRepo(private val items: List<Category>) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = flowOf(items)
    override suspend fun getById(id: String): Category? = items.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category) = Unit
    override suspend fun delete(id: String) = Unit
}

internal class FakeRecurringRepo(private val items: List<RecurringExpense>) : RecurringExpenseRepository {
    override fun observeAll(): Flow<List<RecurringExpense>> = flowOf(items)
    override suspend fun getDue(date: LocalDate): List<RecurringExpense> =
        items.filter { it.autoCreate && !it.nextRunDate.isAfter(date) }
    override suspend fun getById(id: String): RecurringExpense? = items.firstOrNull { it.id == id }
    override suspend fun upsert(recurring: RecurringExpense) = Unit
    override suspend fun delete(id: String) = Unit
}

/**
 * Stateful, following [FakeSavingsRepo]: which method is the default is a fact about the whole set
 * that only a write can change, so a no-op `upsert` could not tell a use case that sets one default
 * from one that leaves two behind.
 *
 * Order is preserved on update rather than moving the row to the end, because the defect being
 * guarded against is a reader falling back to `firstOrNull { it.isDefault }` and answering by
 * position.
 */
internal class FakePmRepo(initial: List<PaymentMethod> = emptyList()) : PaymentMethodRepository {
    private val state = MutableStateFlow(initial)

    fun all(): List<PaymentMethod> = state.value

    override fun observeAll(): Flow<List<PaymentMethod>> = state
    override suspend fun getById(id: String): PaymentMethod? = state.value.firstOrNull { it.id == id }

    override suspend fun upsert(paymentMethod: PaymentMethod) {
        val at = state.value.indexOfFirst { it.id == paymentMethod.id }
        state.value = if (at >= 0) {
            state.value.toMutableList().also { it[at] = paymentMethod }
        } else {
            state.value + paymentMethod
        }
    }

    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }
}

internal class FakeSettings(
    private val currency: String = "PEN",
    private val pinHash: String? = null,
) : SettingsRepository {
    override val preferences: Flow<UserPreferences> =
        flowOf(UserPreferences(currencyCode = currency, pinHash = pinHash))
    override suspend fun setThemeMode(mode: ThemeMode) = Unit
    override suspend fun setCurrency(code: String) = Unit
    override suspend fun setOnboardingComplete(complete: Boolean) = Unit
    override suspend fun setPinHash(hash: String?) = Unit
    override suspend fun setBiometricEnabled(enabled: Boolean) = Unit
    override suspend fun setMonthlyBudget(minor: Long?) = Unit
    override suspend fun setActiveMethodIds(ids: Set<String>?) = Unit
    override suspend fun setShortcuts(shortcuts: List<pe.moneyflow.core.model.QuickShortcut>) = Unit
    override suspend fun setAmountsHidden(hidden: Boolean) = Unit
}
