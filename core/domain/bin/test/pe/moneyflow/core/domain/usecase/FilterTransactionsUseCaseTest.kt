package pe.moneyflow.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.domain.model.TransactionFilter
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

class FilterTransactionsUseCaseTest {

    private val useCase = FilterTransactionsUseCase()

    private val txs = listOf(
        Transaction(id = "1", title = "Almuerzo pollería", notes = "con amigos", amountMinor = 3000, categoryId = "food", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 10)),
        Transaction(id = "2", title = "Sueldo julio", amountMinor = 300000, categoryId = "salary", type = TransactionType.INCOME, actualDate = LocalDate.of(2026, 7, 1)),
        Transaction(id = "3", title = "Taxi al trabajo", amountMinor = 1500, categoryId = "transport", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 6, 28)),
    )

    @Test
    fun `empty filter returns everything`() {
        assertEquals(3, useCase(txs, TransactionFilter()).size)
    }

    @Test
    fun `query matches title case-insensitively`() {
        val result = useCase(txs, TransactionFilter(query = "TAXI"))
        assertEquals(listOf("3"), result.map { it.id })
    }

    @Test
    fun `query matches notes`() {
        val result = useCase(txs, TransactionFilter(query = "amigos"))
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `type filter keeps only matching types`() {
        val result = useCase(txs, TransactionFilter(types = setOf(TransactionType.INCOME)))
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `category filter keeps only matching categories`() {
        val result = useCase(txs, TransactionFilter(categoryIds = setOf("food", "transport")))
        assertEquals(setOf("1", "3"), result.map { it.id }.toSet())
    }

    @Test
    fun `date bounds are inclusive on effective date`() {
        val result = useCase(
            txs,
            TransactionFilter(start = LocalDate.of(2026, 7, 1), end = LocalDate.of(2026, 7, 31)),
        )
        assertEquals(setOf("1", "2"), result.map { it.id }.toSet())
    }

    @Test
    fun `combined query and type narrow together`() {
        val result = useCase(
            txs,
            TransactionFilter(query = "julio", types = setOf(TransactionType.INCOME)),
        )
        assertEquals(listOf("2"), result.map { it.id })
    }
}
