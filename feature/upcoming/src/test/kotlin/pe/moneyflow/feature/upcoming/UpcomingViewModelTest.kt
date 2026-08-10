package pe.moneyflow.feature.upcoming

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.GetUpcomingPaymentsUseCase
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.domain.usecase.SettleUpcomingPaymentUseCase
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakePaymentMethodRepository
import pe.moneyflow.core.testing.FakeRecurringExpenseRepository
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Covers the one rule the handoff calls non-optional: settling a payment **records the method it was
 * paid with**, on every path. Nothing in the UI reveals whether that happened — the only symptom of
 * it breaking is that the suggested method silently stops improving — so it is exactly the behaviour
 * that needs a test rather than a look.
 */
class UpcomingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 2)
    private val clock = Clock.fixed(Instant.parse("2026-08-02T12:00:00Z"), ZoneOffset.UTC)

    private val yape = PaymentMethod(
        id = "yape",
        name = "Yape",
        iconKey = "wallet",
        colorHex = "#7E57C2",
        deepLinkPackage = "com.bcp.innovacxion.yapeapp",
    )
    private val cash = PaymentMethod(id = "cash", name = "Efectivo", iconKey = "cash", colorHex = "#26A69A")

    private val gym = Transaction(
        id = "gym",
        title = "Gimnasio",
        amountMinor = 12_000,
        categoryId = "c1",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = today.minusDays(3),
        // Deliberately unset: settling is what should record it.
        paymentMethodId = null,
    )

    private fun viewModel(repo: FakeTransactionRepository) = UpcomingViewModel(
        getUpcoming = GetUpcomingPaymentsUseCase(
            transactionRepository = repo,
            categoryRepository = FakeCategoryRepository(),
            recurringExpenseRepository = FakeRecurringExpenseRepository(),
            clock = clock,
        ),
        paymentMethodRepository = FakePaymentMethodRepository(listOf(cash, yape)),
        settleUpcomingPayment = SettleUpcomingPaymentUseCase(
            markTransactionPaid = MarkTransactionPaidUseCase(repo, clock),
            saveTransaction = SaveTransactionUseCase(repo),
            deleteTransaction = DeleteTransactionUseCase(repo),
            recurringExpenseRepository = FakeRecurringExpenseRepository(),
            clock = clock,
        ),
        saveTransaction = SaveTransactionUseCase(repo),
        deleteTransaction = DeleteTransactionUseCase(repo),
        clock = clock,
    )

    private fun payment(tx: Transaction, projected: Boolean = false) = UpcomingPayment(
        transaction = tx,
        bucket = UpcomingBucket.DUE_NOW,
        category = null,
        dueDate = tx.estimatedDate,
        isProjected = projected,
    )

    @Test
    fun `settling a real row records the method it was paid with`() = runTest {
        val repo = FakeTransactionRepository(listOf(gym))

        viewModel(repo).settle(payment(gym), methodId = "yape")
        advanceUntilIdle()

        val stored = repo["gym"]!!
        assertEquals(TransactionStatus.PAID, stored.status)
        assertEquals("yape", stored.paymentMethodId)
        assertEquals(today, stored.actualDate)
    }

    /**
     * "Ya pagué por fuera" on a *projected* occurrence has no row to update, so it materializes one.
     * The method has to survive that path too — this is where it is easiest to drop, because the
     * transaction is being built from scratch rather than copied.
     */
    @Test
    fun `settling a projection materializes it as paid, with the method`() = runTest {
        val repo = FakeTransactionRepository(emptyList())
        val projected = gym.copy(id = "", recurringId = "tpl-1")

        viewModel(repo).settle(payment(projected, projected = true), methodId = "yape")
        advanceUntilIdle()

        val stored = repo.all().single()
        assertTrue("a real id should be minted", stored.id.isNotBlank())
        assertEquals(TransactionStatus.PAID, stored.status)
        assertEquals("yape", stored.paymentMethodId)
        assertEquals("tpl-1", stored.recurringId)
    }

    @Test
    fun `settling without a method leaves the payment's own method untouched`() = runTest {
        val repo = FakeTransactionRepository(listOf(gym.copy(paymentMethodId = "cash")))

        viewModel(repo).settle(payment(gym.copy(paymentMethodId = "cash")), methodId = null)
        advanceUntilIdle()

        assertEquals("cash", repo["gym"]!!.paymentMethodId)
    }

    @Test
    fun `undo restores the row exactly, including the method it had before`() = runTest {
        val repo = FakeTransactionRepository(listOf(gym))
        val vm = viewModel(repo)

        vm.settle(payment(gym), methodId = "yape")
        advanceUntilIdle()
        vm.undoSettle()
        advanceUntilIdle()

        val restored = repo["gym"]!!
        assertEquals(TransactionStatus.PENDING, restored.status)
        assertNull("undo must not keep the method the undone settle recorded", restored.paymentMethodId)
    }

    /** Undoing a settled projection deletes the row it created, rather than leaving an orphan. */
    @Test
    fun `undo removes a materialized projection`() = runTest {
        val repo = FakeTransactionRepository(emptyList())
        val vm = viewModel(repo)

        vm.settle(payment(gym.copy(id = "", recurringId = "tpl-1"), projected = true), methodId = "yape")
        advanceUntilIdle()
        vm.undoSettle()
        advanceUntilIdle()

        assertTrue(repo.all().isEmpty())
    }

    @Test
    fun `undo is a one-shot — a second call cannot re-apply it`() = runTest {
        val repo = FakeTransactionRepository(listOf(gym))
        val vm = viewModel(repo)

        vm.settle(payment(gym), methodId = "yape")
        advanceUntilIdle()
        vm.undoSettle()
        advanceUntilIdle()
        vm.undoSettle()
        advanceUntilIdle()

        assertEquals(TransactionStatus.PENDING, repo["gym"]!!.status)
    }

    /**
     * The batch is only safe to offer because its undo covers the whole thing. Settling three bills
     * and being able to revert one of them would be worse than three separate sheets.
     */
    @Test
    fun `settling a batch records the method on every row`() = runTest {
        val rent = gym.copy(id = "rent", title = "Alquiler", amountMinor = 90_000)
        val repo = FakeTransactionRepository(listOf(gym, rent))

        viewModel(repo).settleAll(listOf(payment(gym), payment(rent)), methodId = "yape")
        advanceUntilIdle()

        assertEquals(TransactionStatus.PAID, repo["gym"]!!.status)
        assertEquals(TransactionStatus.PAID, repo["rent"]!!.status)
        assertEquals("yape", repo["gym"]!!.paymentMethodId)
        assertEquals("yape", repo["rent"]!!.paymentMethodId)
    }

    @Test
    fun `one undo reverts the whole batch, not just the last row`() = runTest {
        val rent = gym.copy(id = "rent", title = "Alquiler", amountMinor = 90_000)
        val repo = FakeTransactionRepository(listOf(gym, rent))
        val vm = viewModel(repo)

        vm.settleAll(listOf(payment(gym), payment(rent)), methodId = "yape")
        advanceUntilIdle()

        vm.undoSettle()
        advanceUntilIdle()

        assertEquals(TransactionStatus.PENDING, repo["gym"]!!.status)
        assertEquals(TransactionStatus.PENDING, repo["rent"]!!.status)
        assertNull(repo["gym"]!!.paymentMethodId)
    }

    @Test
    fun `settling an empty batch does nothing and leaves no undo behind`() = runTest {
        val repo = FakeTransactionRepository(listOf(gym))
        val vm = viewModel(repo)

        vm.settleAll(emptyList(), methodId = "yape")
        advanceUntilIdle()
        // The previous undo must not fire for a batch that never happened.
        vm.undoSettle()
        advanceUntilIdle()

        assertEquals(TransactionStatus.PENDING, repo["gym"]!!.status)
    }

    @Test
    fun `deleting stashes the row so undo can restore it verbatim`() = runTest {
        val repo = FakeTransactionRepository(listOf(gym))
        val vm = viewModel(repo)

        vm.delete(gym)
        advanceUntilIdle()
        assertTrue(repo.all().isEmpty())

        vm.undoDelete()
        advanceUntilIdle()
        assertEquals(gym, repo["gym"])
    }
}
