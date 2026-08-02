package pe.moneyflow.feature.budgets

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.DeleteBudgetUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveBudgetUseCase
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeBudgetRepository
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The month-allocation roll-up — how much of the month's budget has been handed to categories, how
 * much is still unassigned, how much spending no budget is watching — and the editing of the month
 * budget itself.
 *
 * The roll-up arithmetic is tested rather than eyeballed because it is derived from three sources at
 * once. The editing is tested because its undo has no symptom beyond a snackbar: nothing on screen
 * distinguishes "undo restored the old amount" from "undo quietly did the wrong thing".
 */
class BudgetsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 15)
    private val clock = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC)

    private val comida = category("comida", "Comida")
    private val transporte = category("transporte", "Transporte")
    private val ocio = category("ocio", "Ocio")

    private fun category(id: String, name: String) = Category(
        id = id,
        name = name,
        iconKey = "category",
        colorHex = "#FF7043",
        type = CategoryType.EXPENSE,
    )

    private fun budget(id: String, categoryId: String, amountMinor: Long) = Budget(
        id = id,
        name = id,
        categoryId = categoryId,
        amountMinor = amountMinor,
        period = BudgetPeriod.MONTHLY,
        startDate = today.withDayOfMonth(1),
    )

    private fun expense(id: String, categoryId: String?, amountMinor: Long, date: LocalDate = today) =
        Transaction(
            id = id,
            title = id,
            amountMinor = amountMinor,
            categoryId = categoryId,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PAID,
            actualDate = date,
        )

    private fun viewModel(
        budgets: List<Budget> = emptyList(),
        transactions: List<Transaction> = emptyList(),
        monthlyBudgetMinor: Long? = 200_000,
        settings: FakeSettingsRepository = FakeSettingsRepository(
            monthlyBudgetMinor = monthlyBudgetMinor,
        ),
    ): BudgetsViewModel {
        val txRepo = FakeTransactionRepository(transactions)
        val catRepo = FakeCategoryRepository(listOf(comida, transporte, ocio))
        val budgetRepo = FakeBudgetRepository(budgets)
        return BudgetsViewModel(
            savedStateHandle = SavedStateHandle(),
            getBudgetsProgress = GetBudgetsProgressUseCase(
                budgetRepo, txRepo, catRepo, settings, clock,
            ),
            observeCategories = ObserveCategoriesUseCase(catRepo),
            observeTransactions = ObserveTransactionsUseCase(txRepo),
            settingsRepository = settings,
            saveBudget = SaveBudgetUseCase(budgetRepo),
            deleteBudget = DeleteBudgetUseCase(budgetRepo),
            clock = clock,
        )
    }

    @Test
    fun `unassigned is the month budget minus what categories were given`() = runTest {
        val state = viewModel(
            budgets = listOf(budget("b1", "comida", 60_000), budget("b2", "transporte", 40_000)),
        ).uiState.first { !it.isLoading }

        assertEquals(100_000L, state.totalLimitMinor)
        assertEquals(100_000L, state.unassignedMinor)
    }

    /**
     * Over-allocating is a legitimate choice — the limits are per-category caps, not slices of a
     * fixed pie — so "unassigned" floors at zero rather than going negative and reading as an error.
     */
    @Test
    fun `over-allocating floors unassigned at zero instead of going negative`() = runTest {
        val state = viewModel(
            budgets = listOf(budget("b1", "comida", 150_000), budget("b2", "transporte", 120_000)),
        ).uiState.first { !it.isLoading }

        assertEquals(270_000L, state.totalLimitMinor)
        assertEquals(0L, state.unassignedMinor)
    }

    @Test
    fun `spending in a category with no budget counts as unbudgeted`() = runTest {
        val state = viewModel(
            budgets = listOf(budget("b1", "comida", 60_000)),
            transactions = listOf(
                expense("a", "comida", 20_000),
                expense("b", "ocio", 15_000),
                expense("c", null, 5_000),
            ),
        ).uiState.first { !it.isLoading }

        assertEquals(40_000L, state.monthSpentMinor)
        assertEquals("ocio + uncategorised, not the budgeted comida", 20_000L, state.unbudgetedSpentMinor)
    }

    /**
     * "Covered" is by category, not by amount: blowing past a limit is an overrun, not unbudgeted
     * spending. Those are different questions and the roll-up answers only the second.
     */
    @Test
    fun `overspending a budgeted category is not unbudgeted spending`() = runTest {
        val state = viewModel(
            budgets = listOf(budget("b1", "comida", 10_000)),
            transactions = listOf(expense("a", "comida", 90_000)),
        ).uiState.first { !it.isLoading }

        assertEquals(90_000L, state.monthSpentMinor)
        assertEquals(0L, state.unbudgetedSpentMinor)
    }

    @Test
    fun `last month's spending is not counted`() = runTest {
        val state = viewModel(
            transactions = listOf(
                expense("thisMonth", "ocio", 10_000),
                expense("lastMonth", "ocio", 99_000, today.minusMonths(1)),
            ),
        ).uiState.first { !it.isLoading }

        assertEquals(10_000L, state.monthSpentMinor)
    }

    @Test
    fun `pending expenses do not count as spent`() = runTest {
        val pending = expense("bill", "ocio", 50_000).copy(
            status = TransactionStatus.PENDING,
            actualDate = null,
            estimatedDate = today,
        )
        val state = viewModel(
            transactions = listOf(expense("paid", "ocio", 10_000), pending),
        ).uiState.first { !it.isLoading }

        assertEquals(10_000L, state.monthSpentMinor)
    }

    @Test
    fun `without a month budget there is nothing to divide, so no roll-up`() = runTest {
        val state = viewModel(
            budgets = listOf(budget("b1", "comida", 60_000)),
            monthlyBudgetMinor = null,
        ).uiState.first { !it.isLoading }

        assertFalse(state.hasRollup)
        assertEquals(null, state.unassignedMinor)
    }

    @Test
    fun `a month budget with no categories yet still shows the roll-up`() = runTest {
        val state = viewModel(budgets = emptyList()).uiState.first { !it.isLoading }

        assertTrue("this is exactly when 'you have assigned none of it' is worth saying", state.hasRollup)
        assertTrue(state.isEmpty)
        assertEquals(200_000L, state.unassignedMinor)
    }

    // -----------------------------------------------------------------------------------------
    // Editing the month budget. Until this existed the value could only be written by onboarding,
    // so these cover the path that made it recoverable — and its undo, which has no UI symptom
    // beyond the snackbar and so cannot be confirmed by looking.
    // -----------------------------------------------------------------------------------------

    @Test
    fun `setting the month budget persists it`() = runTest {
        val settings = FakeSettingsRepository(monthlyBudgetMinor = null)
        val vm = viewModel(settings = settings)

        vm.setMonthlyBudget(200_000)
        advanceUntilIdle()

        assertEquals(200_000L, settings.current.monthlyBudgetMinor)
    }

    @Test
    fun `undo restores the previous amount after a change`() = runTest {
        val settings = FakeSettingsRepository(monthlyBudgetMinor = 200_000)
        val vm = viewModel(settings = settings)
        vm.uiState.first { !it.isLoading }

        vm.setMonthlyBudget(350_000)
        advanceUntilIdle()
        assertEquals(350_000L, settings.current.monthlyBudgetMinor)

        vm.undoMonthlyBudget()
        advanceUntilIdle()

        assertEquals(200_000L, settings.current.monthlyBudgetMinor)
    }

    /** Clearing is the destructive path, so putting the amount back is the whole point of the undo. */
    @Test
    fun `undo restores the amount after clearing`() = runTest {
        val settings = FakeSettingsRepository(monthlyBudgetMinor = 200_000)
        val vm = viewModel(settings = settings)
        vm.uiState.first { !it.isLoading }

        vm.setMonthlyBudget(null)
        advanceUntilIdle()
        assertNull(settings.current.monthlyBudgetMinor)

        vm.undoMonthlyBudget()
        advanceUntilIdle()

        assertEquals(200_000L, settings.current.monthlyBudgetMinor)
    }

    /**
     * Undoing a *first* set returns to having none — the state really was null before, and the UI
     * correspondingly does not offer undo on that path.
     */
    @Test
    fun `undo after the first ever set clears it again`() = runTest {
        val settings = FakeSettingsRepository(monthlyBudgetMinor = null)
        val vm = viewModel(settings = settings)
        vm.uiState.first { !it.isLoading }

        vm.setMonthlyBudget(200_000)
        advanceUntilIdle()
        vm.undoMonthlyBudget()
        advanceUntilIdle()

        assertNull(settings.current.monthlyBudgetMinor)
    }

    @Test
    fun `undo is a one-shot — a second call cannot re-apply it`() = runTest {
        val settings = FakeSettingsRepository(monthlyBudgetMinor = 200_000)
        val vm = viewModel(settings = settings)
        vm.uiState.first { !it.isLoading }

        vm.setMonthlyBudget(350_000)
        advanceUntilIdle()
        vm.undoMonthlyBudget()
        advanceUntilIdle()
        vm.undoMonthlyBudget()
        advanceUntilIdle()

        assertEquals(
            "a second undo must not re-apply the value the first one reverted",
            200_000L,
            settings.current.monthlyBudgetMinor,
        )
    }

    @Test
    fun `the new amount reaches the roll-up`() = runTest {
        val settings = FakeSettingsRepository(monthlyBudgetMinor = null)
        val vm = viewModel(budgets = listOf(budget("b1", "comida", 60_000)), settings = settings)
        assertFalse(vm.uiState.first { !it.isLoading }.hasRollup)

        vm.setMonthlyBudget(200_000)
        advanceUntilIdle()

        val state = vm.uiState.first { !it.isLoading }
        assertTrue(state.hasRollup)
        assertEquals(140_000L, state.unassignedMinor)
    }
}
