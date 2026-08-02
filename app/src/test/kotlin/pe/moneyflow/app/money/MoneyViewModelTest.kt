package pe.moneyflow.app.money

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetNetWorthUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.ObserveSavingsGoalsUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.SavingsGoal
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeAccountRepository
import pe.moneyflow.core.testing.FakeBudgetRepository
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakeExchangeRateRepository
import pe.moneyflow.core.testing.FakePaymentMethodRepository
import pe.moneyflow.core.testing.FakeRecurringExpenseRepository
import pe.moneyflow.core.testing.FakeSavingsGoalRepository
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * "Tu dinero" exists to answer five questions without entering any of its rows, so the figures on
 * those rows are the whole feature — a chevron with a wrong number beside it is worse than a chevron
 * alone.
 */
class MoneyViewModelTest {

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

    private fun viewModel(
        transactions: List<Transaction> = emptyList(),
        budgets: List<Budget> = emptyList(),
        accounts: List<Account> = emptyList(),
        goals: List<SavingsGoal> = emptyList(),
        methods: List<PaymentMethod> = emptyList(),
    ): MoneyViewModel {
        val txRepo = FakeTransactionRepository(transactions)
        val catRepo = FakeCategoryRepository(listOf(comida))
        val settings = FakeSettingsRepository()
        return MoneyViewModel(
            getBudgetsProgress = GetBudgetsProgressUseCase(
                FakeBudgetRepository(budgets), txRepo, catRepo, settings, clock,
            ),
            getUpcomingPayments = GetUpcomingPaymentsUseCase(
                txRepo, catRepo, FakeRecurringExpenseRepository(), clock,
            ),
            getNetWorth = GetNetWorthUseCase(
                FakeAccountRepository(accounts), txRepo, FakeExchangeRateRepository(), settings,
            ),
            observeSavingsGoals = ObserveSavingsGoalsUseCase(FakeSavingsGoalRepository(goals)),
            paymentMethodRepository = FakePaymentMethodRepository(methods),
        )
    }

    private fun pending(id: String, amountMinor: Long, due: LocalDate) = Transaction(
        id = id,
        title = id,
        amountMinor = amountMinor,
        categoryId = "comida",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = due,
    )

    @Test
    fun `an empty setup reports zeroes rather than staying loading`() = runTest {
        val state = viewModel().uiState.first { !it.isLoading }

        assertEquals(0, state.budgetsAtRisk)
        assertEquals(0L, state.upcomingTotalMinor)
        assertEquals(0, state.overdueCount)
        assertEquals(0, state.methodsCount)
    }

    /** "At risk" is ≥80% of the limit, so a budget merely in use must not raise the count. */
    @Test
    fun `only budgets at or past the warning threshold count as at risk`() = runTest {
        val spent = Transaction(
            id = "t1",
            title = "lunch",
            amountMinor = 8_500,
            categoryId = "comida",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PAID,
            actualDate = today,
        )
        val state = viewModel(
            transactions = listOf(spent),
            budgets = listOf(
                // 85% used — at risk.
                Budget(
                    id = "b1",
                    name = "Comida",
                    categoryId = "comida",
                    amountMinor = 10_000,
                    period = BudgetPeriod.MONTHLY,
                    startDate = today.withDayOfMonth(1),
                ),
                // Nothing spent against it — not at risk.
                Budget(
                    id = "b2",
                    name = "Otros",
                    categoryId = "otros",
                    amountMinor = 50_000,
                    period = BudgetPeriod.MONTHLY,
                    startDate = today.withDayOfMonth(1),
                ),
            ),
        ).uiState.first { !it.isLoading }

        assertEquals(1, state.budgetsAtRisk)
    }

    @Test
    fun `upcoming totals every pending payment and counts only the overdue ones`() = runTest {
        val state = viewModel(
            transactions = listOf(
                pending("late", 12_000, today.minusDays(5)),
                pending("alsoLate", 3_000, today.minusDays(1)),
                pending("soon", 5_000, today.plusDays(6)),
            ),
        ).uiState.first { !it.isLoading }

        assertEquals(20_000L, state.upcomingTotalMinor)
        assertEquals(2, state.overdueCount)
    }

    @Test
    fun `savings sums the goals' current amounts, not their targets`() = runTest {
        val state = viewModel(
            goals = listOf(
                SavingsGoal(id = "g1", name = "Viaje", targetAmountMinor = 500_000, currentAmountMinor = 120_000),
                SavingsGoal(id = "g2", name = "Emergencia", targetAmountMinor = 900_000, currentAmountMinor = 80_000),
            ),
        ).uiState.first { !it.isLoading }

        assertEquals(200_000L, state.savingsBalanceMinor)
    }

    @Test
    fun `methods count reflects the configured payment methods`() = runTest {
        val state = viewModel(
            methods = listOf(
                PaymentMethod(id = "cash", name = "Efectivo", iconKey = "cash", colorHex = "#26A69A"),
                PaymentMethod(id = "yape", name = "Yape", iconKey = "wallet", colorHex = "#7E57C2"),
            ),
        ).uiState.first { !it.isLoading }

        assertEquals(2, state.methodsCount)
    }
}
