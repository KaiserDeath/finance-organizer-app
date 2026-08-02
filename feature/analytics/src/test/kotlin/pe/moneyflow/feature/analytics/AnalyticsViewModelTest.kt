package pe.moneyflow.feature.analytics

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.ExportTransactionsCsvUseCase
import pe.moneyflow.core.domain.usecase.GetAnalyticsUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import pe.moneyflow.core.domain.usecase.GetMonthlyReportUseCase
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeBudgetRepository
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakePaymentMethodRepository
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.FakeSmartInsights
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The card Análisis opens with: which budget is worst, and whether it is one that can be cut.
 *
 * Both are decisions rather than arithmetic — "worst" is the biggest absolute overspend, not the
 * biggest budget or the highest percentage — so they are worth pinning down.
 */
class AnalyticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 15)
    private val clock = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC)

    private val comida = Category(
        id = "comida",
        name = "Comida",
        iconKey = "food",
        colorHex = "#FF7043",
        type = CategoryType.EXPENSE,
    )

    /** Rent: a fixed expense, which the overrun card must treat differently from discretionary spend. */
    private val alquiler = Category(
        id = "alquiler",
        name = "Alquiler",
        iconKey = "home",
        colorHex = "#7E57C2",
        type = CategoryType.EXPENSE,
        isFixed = true,
    )

    private fun expense(id: String, categoryId: String, amountMinor: Long, day: Int = 5) = Transaction(
        id = id,
        title = id,
        amountMinor = amountMinor,
        categoryId = categoryId,
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PAID,
        actualDate = today.withDayOfMonth(day),
    )

    private fun budget(id: String, categoryId: String, amountMinor: Long) = Budget(
        id = id,
        name = id,
        categoryId = categoryId,
        amountMinor = amountMinor,
        period = BudgetPeriod.MONTHLY,
        startDate = today.withDayOfMonth(1),
    )

    private fun viewModel(
        transactions: List<Transaction> = emptyList(),
        budgets: List<Budget> = emptyList(),
        categories: List<Category> = listOf(comida, alquiler),
    ): AnalyticsViewModel {
        val txRepo = FakeTransactionRepository(transactions)
        val catRepo = FakeCategoryRepository(categories)
        val settings = FakeSettingsRepository()
        return AnalyticsViewModel(
            getAnalytics = GetAnalyticsUseCase(txRepo, catRepo, settings, clock),
            getMonthlyReport = GetMonthlyReportUseCase(txRepo, catRepo, settings, clock),
            getBudgetsProgress = GetBudgetsProgressUseCase(
                FakeBudgetRepository(budgets), txRepo, catRepo, settings, clock,
            ),
            getInsights = GetInsightsUseCase(FakeSmartInsights()),
            exportCsv = ExportTransactionsCsvUseCase(
                txRepo, catRepo, FakePaymentMethodRepository(), clock,
            ),
        )
    }

    @Test
    fun `no budget over its limit means no overrun, but still something to watch`() = runTest {
        val state = viewModel(
            transactions = listOf(expense("lunch", "comida", 5_000)),
            budgets = listOf(budget("b1", "comida", 50_000)),
        ).uiState.first { !it.isLoading }

        assertNull(state.worstOverrun)
        assertEquals("b1", state.closestToLimit?.budget?.id)
    }

    /**
     * "Closest" is the highest *fraction* of its limit, not the biggest spend: a budget at 90% of
     * S/ 1,000 is closer to trouble than one at 10% of S/ 500, despite spending nine times more.
     */
    @Test
    fun `closest to the limit is the highest fraction, not the biggest spend`() = runTest {
        val state = viewModel(
            transactions = listOf(
                expense("rent", "alquiler", 90_000),
                expense("lunch", "comida", 5_000),
            ),
            budgets = listOf(budget("b1", "comida", 50_000), budget("b2", "alquiler", 100_000)),
        ).uiState.first { !it.isLoading }

        assertEquals("b2", state.closestToLimit?.budget?.id)
    }

    /** An overrun is the other card's job — it must not also show up as the thing to watch. */
    @Test
    fun `a budget already over its limit is not the closest to it`() = runTest {
        val state = viewModel(
            transactions = listOf(
                expense("rent", "alquiler", 110_000),
                expense("lunch", "comida", 5_000),
            ),
            budgets = listOf(budget("b1", "comida", 50_000), budget("b2", "alquiler", 100_000)),
        ).uiState.first { !it.isLoading }

        assertEquals("b2", state.worstOverrun?.budget?.id)
        assertEquals("b1", state.closestToLimit?.budget?.id)
    }

    /** No budgets at all is a third state: nothing to watch, so the card keeps plain reassurance. */
    @Test
    fun `no budgets at all leaves nothing to watch`() = runTest {
        val state = viewModel(
            transactions = listOf(expense("lunch", "comida", 5_000)),
        ).uiState.first { !it.isLoading }

        assertNull(state.worstOverrun)
        assertNull(state.closestToLimit)
    }

    @Test
    fun `the worst overrun is the biggest absolute overspend, not the biggest budget`() = runTest {
        val state = viewModel(
            transactions = listOf(
                // Over by 2,000 on a small budget.
                expense("lunch", "comida", 12_000),
                // Over by 10,000 on a large one — this is the one to surface.
                expense("rent", "alquiler", 110_000),
            ),
            budgets = listOf(budget("b1", "comida", 10_000), budget("b2", "alquiler", 100_000)),
        ).uiState.first { !it.isLoading }

        assertEquals("b2", state.worstOverrun?.budget?.id)
    }

    /**
     * The fixed-expense variant is the reason `isFixedExpense` exists: rent that blew its budget
     * cannot be cut, so the card drops the days-left pressure and offers to correct the limit.
     */
    @Test
    fun `a fixed-expense overrun is flagged as fixed`() = runTest {
        val state = viewModel(
            transactions = listOf(expense("rent", "alquiler", 110_000)),
            budgets = listOf(budget("b2", "alquiler", 100_000)),
        ).uiState.first { !it.isLoading }

        assertTrue(state.worstOverrun!!.isFixedExpense)
    }

    @Test
    fun `a discretionary overrun is not flagged as fixed`() = runTest {
        val state = viewModel(
            transactions = listOf(expense("lunch", "comida", 12_000)),
            budgets = listOf(budget("b1", "comida", 10_000)),
        ).uiState.first { !it.isLoading }

        assertFalse(state.worstOverrun!!.isFixedExpense)
    }

    @Test
    fun `totals reach the state`() = runTest {
        val state = viewModel(
            transactions = listOf(expense("a", "comida", 3_000), expense("b", "comida", 7_000)),
        ).uiState.first { !it.isLoading }

        assertEquals(10_000L, state.analytics.totalExpenseMinor)
    }
}
