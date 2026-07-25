package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GenerateDueRecurringUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)
    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    private fun useCase(
        recurring: InMemoryRecurringRepository,
        transactions: RecordingTransactionRepository,
    ) = GenerateDueRecurringUseCase(recurring, transactions, clock)

    @Test
    fun `materializes a due template as a pending transaction and advances next run`() = runTest {
        val template = RecurringExpense(
            id = "r1",
            title = "Alquiler",
            amountMinor = 120000,
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            nextRunDate = today, // due today
        )
        val recurring = InMemoryRecurringRepository(listOf(template))
        val transactions = RecordingTransactionRepository()

        val created = useCase(recurring, transactions).invoke()

        assertEquals(1, created)
        val tx = transactions.saved.single()
        assertEquals(TransactionStatus.PENDING, tx.status)
        assertEquals(today, tx.estimatedDate)
        assertEquals("r1", tx.recurringId)
        // Next run advanced one month past today, into the future.
        assertEquals(today.plusMonths(1), recurring.byId("r1").nextRunDate)
    }

    @Test
    fun `backfills every missed occurrence when a template is overdue`() = runTest {
        val template = RecurringExpense(
            id = "r2",
            title = "Netflix",
            amountMinor = 3490,
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 1,
            nextRunDate = today.minusWeeks(3), // 3 weeks late → weeks at -3,-2,-1,0 = 4 occurrences
        )
        val recurring = InMemoryRecurringRepository(listOf(template))
        val transactions = RecordingTransactionRepository()

        val created = useCase(recurring, transactions).invoke()

        assertEquals(4, created)
        assertTrue(recurring.byId("r2").nextRunDate.isAfter(today))
    }

    @Test
    fun `is idempotent on a second run the same day`() = runTest {
        val recurring = InMemoryRecurringRepository(
            listOf(RecurringExpense(id = "r3", title = "Gym", amountMinor = 8000, nextRunDate = today)),
        )
        val transactions = RecordingTransactionRepository()
        val uc = useCase(recurring, transactions)

        val first = uc()
        val second = uc() // next run is now in the future → nothing due

        assertEquals(1, first)
        assertEquals(0, second)
        assertEquals(1, transactions.saved.size)
    }

    @Test
    fun `stops at end date`() = runTest {
        val template = RecurringExpense(
            id = "r4",
            title = "Plan corto",
            amountMinor = 5000,
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            nextRunDate = today.minusMonths(2),
            endDate = today.minusMonths(1), // only the two months up to endDate qualify
        )
        val recurring = InMemoryRecurringRepository(listOf(template))
        val transactions = RecordingTransactionRepository()

        val created = useCase(recurring, transactions).invoke()

        assertEquals(2, created)
    }
}

private class InMemoryRecurringRepository(initial: List<RecurringExpense>) : RecurringExpenseRepository {
    private val store = initial.associateBy { it.id }.toMutableMap()

    fun byId(id: String): RecurringExpense = store.getValue(id)

    override fun observeAll(): Flow<List<RecurringExpense>> = flowOf(store.values.toList())
    override suspend fun getDue(date: LocalDate): List<RecurringExpense> =
        store.values.filter { it.autoCreate && !it.nextRunDate.isAfter(date) }
    override suspend fun getById(id: String): RecurringExpense? = store[id]
    override suspend fun upsert(recurring: RecurringExpense) { store[recurring.id] = recurring }
    override suspend fun delete(id: String) { store.remove(id) }
}

private class RecordingTransactionRepository : TransactionRepository {
    val saved = mutableListOf<Transaction>()

    override fun observeAll(): Flow<List<Transaction>> = flowOf(saved.toList())
    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        flowOf(emptyList())
    override suspend fun getById(id: String): Transaction? = saved.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction) { saved += transaction }
    override suspend fun delete(id: String) { saved.removeAll { it.id == id } }
}
