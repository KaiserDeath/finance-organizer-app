package pe.moneyflow.feature.dashboard

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase
import pe.moneyflow.core.domain.usecase.GetFrequentShortcutsUseCase
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeBudgetRepository
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakeRecurringExpenseRepository
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.FakeSmartInsights
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The dashboard's two behaviours worth pinning: the one-tap shortcut path (which writes to the
 * ledger with no confirmation step, so its undo has to be exact), and the rule that pace, budgets,
 * shortcuts and the streak exist only for the current month.
 *
 * That last one matters because `GetBudgetsProgressUseCase` evaluates budgets against *today's*
 * period — showing them while viewing July would compare July's spend against August's limits.
 */
class DashboardViewModelTest {

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

    private val almuerzo = QuickShortcut(
        label = "Almuerzo",
        amountMinor = 1_500,
        categoryId = "comida",
        paymentMethodId = "cash",
    )

    private fun expense(id: String, amountMinor: Long, date: LocalDate) = Transaction(
        id = id,
        title = id,
        amountMinor = amountMinor,
        categoryId = "comida",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PAID,
        actualDate = date,
    )

    private fun viewModel(
        repo: FakeTransactionRepository = FakeTransactionRepository(),
        shortcuts: List<QuickShortcut> = emptyList(),
        monthlyBudgetMinor: Long? = null,
    ): DashboardViewModel {
        val catRepo = FakeCategoryRepository(listOf(comida))
        val settings = FakeSettingsRepository(
            monthlyBudgetMinor = monthlyBudgetMinor,
            shortcuts = shortcuts,
        )
        return DashboardViewModel(
            getDashboard = GetDashboardUseCase(repo, catRepo, settings, clock),
            getUpcoming = GetUpcomingPaymentsUseCase(
                repo, catRepo, FakeRecurringExpenseRepository(), clock,
            ),
            getInsights = GetInsightsUseCase(FakeSmartInsights()),
            getBudgetsProgress = GetBudgetsProgressUseCase(
                FakeBudgetRepository(), repo, catRepo, settings, clock,
            ),
            getFrequentShortcuts = GetFrequentShortcutsUseCase(repo, clock),
            observeTransactions = ObserveTransactionsUseCase(repo),
            settingsRepository = settings,
            saveTransaction = SaveTransactionUseCase(repo),
            deleteTransaction = DeleteTransactionUseCase(repo),
            clock = clock,
        )
    }

    @Test
    fun `a shortcut saves a paid expense dated today, carrying its category and method`() = runTest {
        val repo = FakeTransactionRepository()

        viewModel(repo).logShortcut(almuerzo, currencyCode = "PEN")
        advanceUntilIdle()

        val saved = repo.all().single()
        assertEquals("Almuerzo", saved.title)
        assertEquals(1_500L, saved.amountMinor)
        assertEquals(TransactionStatus.PAID, saved.status)
        assertEquals(today, saved.actualDate)
        assertEquals("comida", saved.categoryId)
        assertEquals("cash", saved.paymentMethodId)
    }

    /** The save is automatic, so undo has to remove exactly the row it created — and only that one. */
    @Test
    fun `undoing a shortcut removes only the row it created`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("existing", 9_900, today)))
        val vm = viewModel(repo)

        vm.logShortcut(almuerzo, currencyCode = "PEN")
        advanceUntilIdle()
        assertEquals(2, repo.all().size)

        vm.undoShortcut()
        advanceUntilIdle()

        assertEquals(listOf("existing"), repo.all().map { it.id })
    }

    @Test
    fun `undo is a one-shot — calling it twice does not delete a second row`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("existing", 9_900, today)))
        val vm = viewModel(repo)

        vm.logShortcut(almuerzo, currencyCode = "PEN")
        advanceUntilIdle()
        vm.undoShortcut()
        advanceUntilIdle()
        vm.undoShortcut()
        advanceUntilIdle()

        assertEquals(listOf("existing"), repo.all().map { it.id })
    }

    @Test
    fun `onboarding shortcuts win over the frequency heuristic`() = runTest {
        val state = viewModel(shortcuts = listOf(almuerzo)).uiState.first { !it.isLoading }

        assertEquals(listOf(almuerzo), state.shortcuts)
    }

    @Test
    fun `the current month gets a pace and a streak`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("a", 5_000, today)))

        val state = viewModel(repo, monthlyBudgetMinor = 200_000).uiState.first { !it.isLoading }

        assertNotNull(state.pace)
        assertTrue(state.isCurrentMonth)
        assertTrue(state.streak.isNotEmpty())
    }

    @Test
    fun `a past month has no pace, no streak and no shortcuts`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("a", 5_000, today.minusMonths(1))))
        val vm = viewModel(repo, shortcuts = listOf(almuerzo), monthlyBudgetMinor = 200_000)

        vm.showPreviousMonth()
        val state = vm.uiState.first { !it.isLoading && !it.isCurrentMonth }

        assertNull("budgets are evaluated against today, so a past month must not show a pace", state.pace)
        assertTrue(state.streak.isEmpty())
        assertTrue("logging a shortcut would date it today, not to the month on screen", state.shortcuts.isEmpty())
        assertTrue(state.topBudgets.isEmpty())
    }

    @Test
    fun `forward navigation stops at the current month`() = runTest {
        val vm = viewModel()

        val current = vm.uiState.first { !it.isLoading }
        assertEquals(false, current.canGoForward)

        vm.showPreviousMonth()
        val past = vm.uiState.first { !it.isLoading && !it.isCurrentMonth }
        assertEquals(true, past.canGoForward)
    }
}
