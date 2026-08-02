package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetFrequentShortcutsUseCaseTest {

    private val today = LocalDate.of(2026, 7, 29)
    private val clock = Clock.fixed(Instant.parse("2026-07-29T12:00:00Z"), ZoneOffset.UTC)
    private val useCase = GetFrequentShortcutsUseCase(EmptyRepo, clock)

    private fun expense(
        title: String,
        amount: Long,
        date: LocalDate,
        categoryId: String? = "comida",
        methodId: String? = "yape",
    ) = Transaction(
        id = "$title-$date-$amount",
        title = title,
        amountMinor = amount,
        categoryId = categoryId,
        paymentMethodId = methodId,
        type = TransactionType.EXPENSE,
        actualDate = date,
    )

    /** Anchors history age without being inside the 30-day window. */
    private val oldAnchor = expense("Antiguo", 100, today.minusDays(90))

    @Test
    fun `top four most frequent combos win`() {
        val txs = listOf(oldAnchor) +
            (1..5).map { expense("Almuerzo", 1800, today.minusDays(it.toLong())) } +
            (1..4).map { expense("Pasaje", 500, today.minusDays(it.toLong()), "transporte", "efectivo") } +
            (1..3).map { expense("Café", 800, today.minusDays(it.toLong())) } +
            (1..2).map { expense("Mercado", 9000, today.minusDays(it.toLong()), "comida", "bcp") } +
            listOf(expense("Cine", 2500, today.minusDays(1), "entretenimiento", null))

        val shortcuts = useCase.infer(txs, today)

        assertEquals(listOf("Almuerzo", "Pasaje", "Café", "Mercado"), shortcuts.map { it.label })
        assertEquals("transporte", shortcuts[1].categoryId)
        assertEquals("efectivo", shortcuts[1].paymentMethodId)
    }

    @Test
    fun `amount comes from the most recent occurrence`() {
        val txs = listOf(
            oldAnchor,
            expense("Almuerzo", 1500, today.minusDays(10)),
            expense("Almuerzo", 2000, today.minusDays(1)),
        )

        val shortcuts = useCase.infer(txs, today)

        assertEquals(2000L, shortcuts.first().amountMinor)
    }

    @Test
    fun `less than 30 days of history yields nothing`() {
        val txs = (1..10).map { expense("Almuerzo", 1800, today.minusDays(it.toLong())) }

        assertTrue(useCase.infer(txs, today).isEmpty())
    }

    @Test
    fun `pending and old rows are ignored`() {
        val pending = Transaction(
            id = "p",
            title = "Netflix",
            amountMinor = 4490,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            estimatedDate = today.minusDays(2),
        )
        val txs = listOf(oldAnchor, pending, expense("Vacaciones", 90000, today.minusDays(40)))

        assertTrue(useCase.infer(txs, today).isEmpty())
    }

    private object EmptyRepo : TransactionRepository {
        override fun observeAll(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
            flowOf(emptyList())
        override suspend fun getById(id: String): Transaction? = null
        override suspend fun upsert(transaction: Transaction) = Unit
        override suspend fun delete(id: String) = Unit
    }
}
