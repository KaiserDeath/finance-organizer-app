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
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.domain.usecase.SettleUpcomingPaymentUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeBudgetRepository
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakePaymentMethodRepository
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

    private fun pendingExpense(id: String, amountMinor: Long, date: LocalDate) = Transaction(
        id = id,
        title = id,
        amountMinor = amountMinor,
        categoryId = "comida",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = date,
    )

    private fun viewModel(
        repo: FakeTransactionRepository = FakeTransactionRepository(),
        shortcuts: List<QuickShortcut> = emptyList(),
        monthlyBudgetMinor: Long? = null,
        paymentMethods: List<PaymentMethod> = emptyList(),
        recurringExpenses: List<RecurringExpense> = emptyList(),
        settings: FakeSettingsRepository = FakeSettingsRepository(
            monthlyBudgetMinor = monthlyBudgetMinor,
            shortcuts = shortcuts,
        ),
    ): DashboardViewModel {
        val catRepo = FakeCategoryRepository(listOf(comida))
        return DashboardViewModel(
            getDashboard = GetDashboardUseCase(repo, catRepo, settings, clock),
            getUpcoming = GetUpcomingPaymentsUseCase(
                repo, catRepo, FakeRecurringExpenseRepository(recurringExpenses), clock,
            ),
            getInsights = GetInsightsUseCase(FakeSmartInsights()),
            getBudgetsProgress = GetBudgetsProgressUseCase(
                FakeBudgetRepository(), repo, catRepo, settings, clock,
            ),
            getFrequentShortcuts = GetFrequentShortcutsUseCase(repo, clock),
            observeTransactions = ObserveTransactionsUseCase(repo),
            paymentMethodRepository = FakePaymentMethodRepository(paymentMethods),
            settingsRepository = settings,
            saveTransaction = SaveTransactionUseCase(repo),
            deleteTransaction = DeleteTransactionUseCase(repo),
            settleUpcomingPayment = SettleUpcomingPaymentUseCase(
                markTransactionPaid = MarkTransactionPaidUseCase(repo, clock),
                saveTransaction = SaveTransactionUseCase(repo),
                deleteTransaction = DeleteTransactionUseCase(repo),
                clock = clock,
            ),
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

    /**
     * The first-run collapse replaces three cards with one ask, so it has to be exactly right about
     * when it applies: too eager and it hides a real ledger.
     */
    @Test
    fun `an empty current month is a first run`() = runTest {
        val state = viewModel().uiState.first { !it.isLoading }

        assertTrue(state.isFirstRun)
    }

    @Test
    fun `one movement ends the first run`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("a", 5_000, today)))

        val state = viewModel(repo).uiState.first { !it.isLoading }

        assertEquals(false, state.isFirstRun)
    }

    @Test
    fun `a pending-only ledger still shows the normal dashboard`() = runTest {
        val repo = FakeTransactionRepository(listOf(pendingExpense("due", 5_000, today)))

        val state = viewModel(repo).uiState.first { !it.isLoading }

        assertEquals(false, state.isFirstRun)
        assertNotNull(state.upcomingNudge)
    }

    /**
     * Pins the nudge's overdue/due-soon split: a charge past its date counts as overdue and one
     * within the week-out cutoff counts as due-soon, never both and never neither.
     */
    @Test
    fun `overdue and due-soon pending charges are split correctly in the nudge`() = runTest {
        val repo = FakeTransactionRepository(
            listOf(
                pendingExpense("late", 3_000, today.minusDays(2)),
                pendingExpense("soon", 4_000, today.plusDays(3)),
            ),
        )

        val nudge = viewModel(repo).uiState.first { !it.isLoading }.upcomingNudge

        assertNotNull(nudge)
        assertEquals(1, nudge!!.overdueCount)
        assertEquals(1, nudge.dueSoonCount)
    }

    /**
     * Pins the reversal recorded in docs/design-decisions.md: Inicio's nudge now includes a
     * recurring occurrence projected for the month on screen, so it agrees with Próximos instead of
     * hiding a real commitment until it materializes. The nudge previously excluded every projection.
     */
    @Test
    fun `a recurring occurrence projected for the current month is included in the nudge`() = runTest {
        val template = RecurringExpense(
            id = "rent",
            title = "Alquiler",
            amountMinor = 150_000,
            categoryId = "comida",
            daysOfMonth = listOf(28),
            interval = 1,
            nextRunDate = LocalDate.of(2026, 8, 28),
        )

        val nudge = viewModel(recurringExpenses = listOf(template))
            .uiState.first { !it.isLoading }.upcomingNudge

        assertNotNull(nudge)
        assertEquals(1, nudge!!.payments.size)
        assertTrue(nudge.payments.single().isProjected)
    }

    /**
     * The same template, from a *past* month view, must not surface a next-month projection: the
     * nudge only pulls in projections for the month the user is actually looking at.
     */
    @Test
    fun `a projection is not pulled into the nudge for a month other than the one on screen`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("a", 5_000, today.minusMonths(1))))
        val template = RecurringExpense(
            id = "rent",
            title = "Alquiler",
            amountMinor = 150_000,
            categoryId = "comida",
            daysOfMonth = listOf(28),
            interval = 1,
            nextRunDate = LocalDate.of(2026, 8, 28),
        )
        val vm = viewModel(repo, recurringExpenses = listOf(template))

        vm.showPreviousMonth()
        val state = vm.uiState.first { !it.isLoading && !it.isCurrentMonth }

        assertNull(state.upcomingNudge)
    }

    /**
     * An empty *past* month is just an empty past month — the card asks for a first expense and the
     * FAB it points at would date that expense today, on a different month than the one on screen.
     */
    @Test
    fun `an empty past month is not a first run`() = runTest {
        val repo = FakeTransactionRepository(listOf(expense("a", 5_000, today)))
        val vm = viewModel(repo)

        vm.showPreviousMonth()
        val state = vm.uiState.first { !it.isLoading && !it.isCurrentMonth }

        assertEquals(false, state.isFirstRun)
    }

    /**
     * Discreet mode is persisted, not held in the ViewModel, so the toggle has to reach the store —
     * a flag kept in memory would look identical until the app restarted.
     */
    @Test
    fun `toggling discreet mode writes through to settings`() = runTest {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settings = settings)

        vm.toggleAmountsHidden()
        advanceUntilIdle()
        assertEquals(true, settings.current.amountsHidden)

        vm.toggleAmountsHidden()
        advanceUntilIdle()
        assertEquals(false, settings.current.amountsHidden)
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
