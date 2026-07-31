package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.model.UserPreferences
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetDashboardUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)
    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    private val categories = listOf(
        Category(id = "c1", name = "Comida", iconKey = "food", colorHex = "#FF7043", type = CategoryType.EXPENSE),
    )

    private val transactions = listOf(
        Transaction(id = "1", title = "Almuerzo", amountMinor = 1000, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = today),
        Transaction(id = "2", title = "Café", amountMinor = 500, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = today.minusDays(2)),
        Transaction(id = "3", title = "Sueldo", amountMinor = 300000, type = TransactionType.INCOME, actualDate = today),
    )

    @Test
    fun `computes month, today and income totals`() = runTest {
        val useCase = GetDashboardUseCase(
            transactionRepository = FakeTransactionRepository(transactions),
            categoryRepository = FakeCategoryRepository(categories),
            settingsRepository = FakeSettingsRepository(),
            clock = clock,
        )

        val data = useCase().first()

        assertEquals(1500L, data.monthSpentMinor)
        assertEquals(1000L, data.todaySpentMinor)
        assertEquals(300000L, data.monthIncomeMinor)
        assertEquals(1, data.categoryBreakdown.size)
        assertEquals(1500L, data.categoryBreakdown.first().amountMinor)
    }

    @Test
    fun `pending expenses stay out of the month total`() = runTest {
        val withPending = transactions + Transaction(
            id = "4",
            title = "Netflix",
            amountMinor = 4490,
            categoryId = "c1",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            estimatedDate = today.plusDays(3),
        )
        val useCase = GetDashboardUseCase(
            transactionRepository = FakeTransactionRepository(withPending),
            categoryRepository = FakeCategoryRepository(categories),
            settingsRepository = FakeSettingsRepository(),
            clock = clock,
        )

        val data = useCase().first()

        // "Total del mes" is what was spent; the pending bill surfaces as committed, not here.
        assertEquals(1500L, data.monthSpentMinor)
        assertEquals(1500L, data.categoryBreakdown.sumOf { it.amountMinor })
    }
}

private class FakeTransactionRepository(private val items: List<Transaction>) : TransactionRepository {
    override fun observeAll(): Flow<List<Transaction>> = flowOf(items)
    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        flowOf(items.filter { tx -> tx.effectiveDate?.let { it in start..end } == true })
    override suspend fun getById(id: String): Transaction? = items.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction) = Unit
    override suspend fun delete(id: String) = Unit
}

private class FakeCategoryRepository(private val items: List<Category>) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = flowOf(items)
    override suspend fun getById(id: String): Category? = items.firstOrNull { it.id == id }
    override suspend fun upsert(category: Category) = Unit
    override suspend fun delete(id: String) = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    override val preferences: Flow<UserPreferences> = flowOf(UserPreferences(currencyCode = "PEN"))
    override suspend fun setThemeMode(mode: ThemeMode) = Unit
    override suspend fun setDynamicColor(enabled: Boolean) = Unit
    override suspend fun setCurrency(code: String) = Unit
    override suspend fun setOnboardingComplete(complete: Boolean) = Unit
    override suspend fun setPinHash(hash: String?) = Unit
    override suspend fun setBiometricEnabled(enabled: Boolean) = Unit
    override suspend fun setMonthlyBudget(minor: Long?) = Unit
    override suspend fun setActiveMethodIds(ids: Set<String>?) = Unit
    override suspend fun setShortcuts(shortcuts: List<pe.moneyflow.core.model.QuickShortcut>) = Unit
}
