package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetUpcomingPaymentsUseCaseTest {

    private val today = LocalDate.of(2026, 7, 15)
    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    private fun useCase(
        transactions: List<Transaction> = emptyList(),
        templates: List<RecurringExpense> = emptyList(),
        categories: List<Category> = emptyList(),
    ) = GetUpcomingPaymentsUseCase(
        transactionRepository = FakeTxRepo(transactions),
        categoryRepository = FakeCatRepo(categories),
        recurringExpenseRepository = FakeRecurringRepo(templates),
        clock = clock,
    )

    private fun monthly(
        id: String,
        day: Int,
        nextRun: LocalDate,
        endDate: LocalDate? = null,
        autoCreate: Boolean = true,
        type: TransactionType = TransactionType.EXPENSE,
    ) = RecurringExpense(
        id = id,
        title = "Alquiler-$id",
        amountMinor = 100_00,
        daysOfMonth = listOf(day),
        interval = 1,
        nextRunDate = nextRun,
        endDate = endDate,
        autoCreate = autoCreate,
        type = type,
    )

    private fun pending(id: String, date: LocalDate?, recurringId: String? = null) = Transaction(
        id = id,
        title = "Pago-$id",
        amountMinor = 100_00,
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = date,
        recurringId = recurringId,
    )

    @Test
    fun `projects occurrences through the end of next month and no further`() = runTest {
        // Monthly on the 20th: Jul 20 (this month) and Aug 20 (next month) are in horizon; Sep is not.
        val result = useCase(templates = listOf(monthly("r1", 20, LocalDate.of(2026, 7, 20)))).invoke().first()

        assertEquals(listOf(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 20)), result.map { it.dueDate })
        assertTrue(result.all { it.isProjected })
        assertEquals(
            listOf(UpcomingBucket.THIS_MONTH, UpcomingBucket.NEXT_MONTH),
            result.map { it.bucket },
        )
    }

    @Test
    fun `does not project an occurrence already materialized in the ledger`() = runTest {
        val result = useCase(
            transactions = listOf(pending("t1", LocalDate.of(2026, 7, 20), recurringId = "r1")),
            templates = listOf(monthly("r1", 20, LocalDate.of(2026, 7, 20))),
        ).invoke().first()

        // Jul 20 comes from the real row; only Aug 20 is projected.
        assertEquals(2, result.size)
        val july = result.single { it.dueDate == LocalDate.of(2026, 7, 20) }
        assertFalse(july.isProjected)
        assertEquals("t1", july.transaction.id)
        assertTrue(result.single { it.dueDate == LocalDate.of(2026, 8, 20) }.isProjected)
    }

    @Test
    fun `does not project an occurrence already paid early`() = runTest {
        val paidEarly = pending("t1", LocalDate.of(2026, 8, 20), recurringId = "r1").copy(
            status = TransactionStatus.PAID,
            actualDate = LocalDate.of(2026, 8, 20),
        )
        val result = useCase(
            transactions = listOf(paidEarly),
            templates = listOf(monthly("r1", 20, LocalDate.of(2026, 8, 20))),
        ).invoke().first()

        // The paid row is not pending, so it is not listed — and it must not come back as a forecast.
        assertTrue(result.isEmpty())
    }

    /**
     * Regression for the "pending payment gets counted as an expense but never disappears from
     * Próximos/Inicio" bug: settling a *projected* occurrence stamps `actualDate` with the day it
     * was actually paid, which is rarely the due date itself. The dedup used to key off
     * `effectiveDate` (`actualDate ?: estimatedDate`), so a bill paid on a different day than it was
     * due produced a ledger key the projector's own due-date output never matched, and the same
     * "pending" occurrence kept reappearing next to the real, already-paid row.
     */
    @Test
    fun `does not project an occurrence paid on a different date than it was due`() = runTest {
        val paidNineDaysEarly = pending("t1", LocalDate.of(2026, 8, 20), recurringId = "r1").copy(
            status = TransactionStatus.PAID,
            actualDate = LocalDate.of(2026, 8, 11),
        )
        val result = useCase(
            transactions = listOf(paidNineDaysEarly),
            templates = listOf(monthly("r1", 20, LocalDate.of(2026, 8, 20))),
        ).invoke().first()

        assertTrue(
            "a bill paid ahead of its due date must not reappear as a duplicate projection for it",
            result.isEmpty(),
        )
    }

    @Test
    fun `end date truncates the projection`() = runTest {
        val result = useCase(
            templates = listOf(
                monthly("r1", 20, LocalDate.of(2026, 7, 20), endDate = LocalDate.of(2026, 7, 31)),
            ),
        ).invoke().first()

        assertEquals(listOf(LocalDate.of(2026, 7, 20)), result.map { it.dueDate })
    }

    @Test
    fun `a template whose end date has passed projects nothing`() = runTest {
        val result = useCase(
            templates = listOf(
                monthly("r1", 20, LocalDate.of(2026, 7, 20), endDate = LocalDate.of(2026, 6, 30)),
            ),
        ).invoke().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `manual templates are projected too since they never materialize on their own`() = runTest {
        val result = useCase(
            templates = listOf(monthly("r1", 20, LocalDate.of(2026, 7, 20), autoCreate = false)),
        ).invoke().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isProjected })
    }

    @Test
    fun `income templates are excluded from a list of what you owe`() = runTest {
        val result = useCase(
            templates = listOf(
                monthly("salary", 20, LocalDate.of(2026, 7, 20), type = TransactionType.INCOME),
            ),
        ).invoke().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `last day of month projects across a short month`() = runTest {
        val template = RecurringExpense(
            id = "eom",
            title = "Renta",
            amountMinor = 90000,
            daysOfMonth = listOf(RecurringExpense.LAST_DAY_OF_MONTH),
            interval = 1,
            nextRunDate = LocalDate.of(2026, 7, 31),
        )

        val result = useCase(templates = listOf(template)).invoke().first()

        assertEquals(listOf(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 31)), result.map { it.dueDate })
    }

    @Test
    fun `a stale daily template still reaches the horizon without hanging`() = runTest {
        val template = RecurringExpense(
            id = "daily",
            title = "Café",
            amountMinor = 500,
            frequency = RecurrenceFrequency.DAILY,
            interval = 1,
            nextRunDate = today.minusMonths(6), // worker hasn't run in half a year
        )

        val result = useCase(templates = listOf(template)).invoke().first()

        // Fast-forwarded past the backlog: nothing before today, and the horizon is reached.
        assertTrue(result.none { it.dueDate!!.isBefore(today) })
        assertEquals(today, result.first().dueDate)
        assertTrue(result.any { it.bucket == UpcomingBucket.NEXT_MONTH })
    }

    @Test
    fun `overdue and near-due real rows share the due-now bucket`() = runTest {
        val result = useCase(
            transactions = listOf(
                pending("late", today.minusDays(3)),
                pending("today", today),
                pending("tomorrow", today.plusDays(1)),
                pending("later", today.plusDays(2)),
            ),
        ).invoke().first()

        val byId = result.associateBy { it.transaction.id }
        assertEquals(UpcomingBucket.DUE_NOW, byId.getValue("late").bucket)
        assertTrue(byId.getValue("late").isOverdue)
        assertEquals(UpcomingBucket.DUE_NOW, byId.getValue("today").bucket)
        assertFalse(byId.getValue("today").isOverdue)
        assertEquals(UpcomingBucket.DUE_NOW, byId.getValue("tomorrow").bucket)
        assertEquals(UpcomingBucket.THIS_MONTH, byId.getValue("later").bucket)
    }

    @Test
    fun `a dateless pending row lands in due now so it gets resolved`() = runTest {
        val result = useCase(transactions = listOf(pending("nodate", null))).invoke().first()

        assertEquals(UpcomingBucket.DUE_NOW, result.single().bucket)
        assertFalse(result.single().isOverdue)
    }

    @Test
    fun `projected payments carry the template's category and id`() = runTest {
        val category = Category(id = "c1", name = "Hogar", iconKey = "home", colorHex = "#FF0000")
        val template = monthly("r1", 20, LocalDate.of(2026, 7, 20)).copy(categoryId = "c1")

        val result = useCase(templates = listOf(template), categories = listOf(category)).invoke().first()

        val first = result.first()
        assertEquals("Hogar", first.category?.name)
        assertEquals("r1", first.transaction.recurringId)
        // Blank id marks it as unsaved — nothing may treat it as a row.
        assertEquals("", first.transaction.id)
        assertEquals(TransactionStatus.PENDING, first.transaction.status)
    }
}
