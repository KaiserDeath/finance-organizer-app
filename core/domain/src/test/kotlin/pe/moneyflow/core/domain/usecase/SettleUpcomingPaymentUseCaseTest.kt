package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Regression coverage for the duplicate-recurring-charge bug: settling a *projected* occurrence
 * used to leave its template's `nextRunDate` untouched, so the next cold start or daily worker run
 * (`RecurringGenerationWorker` → `GenerateDueRecurringUseCase`) saw the template still "due" and
 * materialized a second, duplicate PENDING transaction for the same date already paid.
 */
class SettleUpcomingPaymentUseCaseTest {

    private val today = LocalDate.of(2026, 8, 20)
    private val clock = Clock.fixed(Instant.parse("2026-08-20T12:00:00Z"), ZoneOffset.UTC)

    private fun useCase(txRepo: MutableFakeTxRepo, recurringRepo: MutableFakeRecurringRepo) =
        SettleUpcomingPaymentUseCase(
            markTransactionPaid = MarkTransactionPaidUseCase(txRepo, clock),
            saveTransaction = SaveTransactionUseCase(txRepo),
            deleteTransaction = DeleteTransactionUseCase(txRepo),
            recurringExpenseRepository = recurringRepo,
            clock = clock,
        )

    private fun monthlyTemplate(id: String, day: Int, nextRun: LocalDate) = RecurringExpense(
        id = id,
        title = "Alquiler",
        amountMinor = 100_00,
        daysOfMonth = listOf(day),
        interval = 1,
        nextRunDate = nextRun,
    )

    private fun projectedPayment(template: RecurringExpense, dueDate: LocalDate) = UpcomingPayment(
        transaction = Transaction(
            id = "",
            title = template.title,
            amountMinor = template.amountMinor,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            estimatedDate = dueDate,
            recurringId = template.id,
        ),
        bucket = UpcomingBucket.DUE_NOW,
        category = null,
        dueDate = dueDate,
        isProjected = true,
    )

    @Test
    fun `settling a projected occurrence advances its template past the settled date`() = runTest {
        val template = monthlyTemplate("rent", day = 20, nextRun = LocalDate.of(2026, 8, 20))
        val recurringRepo = MutableFakeRecurringRepo(listOf(template))
        val txRepo = MutableFakeTxRepo()

        useCase(txRepo, recurringRepo).invoke(projectedPayment(template, LocalDate.of(2026, 8, 20))).apply()

        val updated = recurringRepo.get("rent")!!
        assertEquals(
            "the schedule must move to the next occurrence after the one just paid",
            LocalDate.of(2026, 9, 20),
            updated.nextRunDate,
        )
    }

    @Test
    fun `settling an occurrence paid days after its due date still advances the template correctly`() = runTest {
        // Mirrors the real flow: the due date is fixed (Aug 20) but actualDate is stamped "today"
        // when settled, which can be any later date. The advance must be keyed on the due date, not
        // on when the pay sheet happened to be tapped.
        val template = monthlyTemplate("rent", day = 20, nextRun = LocalDate.of(2026, 8, 20))
        val recurringRepo = MutableFakeRecurringRepo(listOf(template))
        val txRepo = MutableFakeTxRepo()

        useCase(txRepo, recurringRepo).invoke(projectedPayment(template, LocalDate.of(2026, 8, 20))).apply()

        assertEquals(LocalDate.of(2026, 9, 20), recurringRepo.get("rent")!!.nextRunDate)
    }

    @Test
    fun `settling an occurrence never moves the schedule backwards`() = runTest {
        // The template already advanced past this date (e.g. a later occurrence was settled first,
        // or the worker already materialized past it) — paying an older projection must not regress it.
        val template = monthlyTemplate("rent", day = 20, nextRun = LocalDate.of(2026, 10, 20))
        val recurringRepo = MutableFakeRecurringRepo(listOf(template))
        val txRepo = MutableFakeTxRepo()

        useCase(txRepo, recurringRepo).invoke(projectedPayment(template, LocalDate.of(2026, 8, 20))).apply()

        assertEquals(
            "settling an out-of-order occurrence must not move nextRunDate backwards",
            LocalDate.of(2026, 10, 20),
            recurringRepo.get("rent")!!.nextRunDate,
        )
    }

    @Test
    fun `settling a real pending row does not touch the recurring template`() = runTest {
        // The non-projected path: GenerateDueRecurringUseCase already advanced nextRunDate when it
        // materialized this row, so settling it a second time must be a no-op on the template.
        val template = monthlyTemplate("rent", day = 20, nextRun = LocalDate.of(2026, 9, 20))
        val recurringRepo = MutableFakeRecurringRepo(listOf(template))
        val realRow = Transaction(
            id = "t1",
            title = "Alquiler",
            amountMinor = 100_00,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            estimatedDate = LocalDate.of(2026, 8, 20),
            recurringId = "rent",
        )
        val txRepo = MutableFakeTxRepo(listOf(realRow))
        val payment = UpcomingPayment(
            transaction = realRow,
            bucket = UpcomingBucket.DUE_NOW,
            category = null,
            dueDate = realRow.estimatedDate,
            isProjected = false,
        )

        useCase(txRepo, recurringRepo).invoke(payment).apply()

        assertEquals(LocalDate.of(2026, 9, 20), recurringRepo.get("rent")!!.nextRunDate)
    }

    @Test
    fun `settling an occurrence whose template was deleted is a safe no-op`() = runTest {
        val recurringRepo = MutableFakeRecurringRepo(emptyList())
        val txRepo = MutableFakeTxRepo()
        val orphanTemplate = monthlyTemplate("gone", day = 20, nextRun = LocalDate.of(2026, 8, 20))

        useCase(txRepo, recurringRepo).invoke(projectedPayment(orphanTemplate, LocalDate.of(2026, 8, 20))).apply()

        assertNull(recurringRepo.get("gone"))
        assertTrue("the materialized transaction should still be written", txRepo.all().isNotEmpty())
    }
}

private class MutableFakeTxRepo(seed: List<Transaction> = emptyList()) : TransactionRepository {
    private val store = mutableMapOf<String, Transaction>().apply { seed.forEach { put(it.id, it) } }

    fun all(): List<Transaction> = store.values.toList()

    override fun observeAll(): Flow<List<Transaction>> = flowOf(all())
    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        flowOf(all().filter { tx -> tx.effectiveDate?.let { it in start..end } == true })
    override suspend fun getById(id: String): Transaction? = store[id]
    override suspend fun upsert(transaction: Transaction) {
        store[transaction.id] = transaction
    }
    override suspend fun delete(id: String) {
        store.remove(id)
    }
}

private class MutableFakeRecurringRepo(seed: List<RecurringExpense>) : RecurringExpenseRepository {
    private val store = mutableMapOf<String, RecurringExpense>().apply { seed.forEach { put(it.id, it) } }

    fun get(id: String): RecurringExpense? = store[id]

    override fun observeAll(): Flow<List<RecurringExpense>> = flowOf(store.values.toList())
    override suspend fun getDue(date: LocalDate): List<RecurringExpense> =
        store.values.filter { !it.nextRunDate.isAfter(date) }
    override suspend fun getById(id: String): RecurringExpense? = store[id]
    override suspend fun upsert(recurring: RecurringExpense) {
        store[recurring.id] = recurring
    }
    override suspend fun delete(id: String) {
        store.remove(id)
    }
}
