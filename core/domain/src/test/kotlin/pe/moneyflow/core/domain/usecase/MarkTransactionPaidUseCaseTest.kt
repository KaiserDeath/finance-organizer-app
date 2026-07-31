package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MarkTransactionPaidUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    /** Replaces by id on upsert, so a mark → unmark round-trip leaves a single, mutated row. */
    private class UpsertingTxRepo(seed: Transaction) : TransactionRepository {
        private val store = mutableMapOf(seed.id to seed)
        override fun observeAll(): Flow<List<Transaction>> = flowOf(store.values.toList())
        override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
            flowOf(store.values.toList())
        override suspend fun getById(id: String): Transaction? = store[id]
        override suspend fun upsert(transaction: Transaction) { store[transaction.id] = transaction }
        override suspend fun delete(id: String) { store.remove(id) }
    }

    private fun pending() = Transaction(
        id = "tx-1",
        title = "Alquiler",
        amountMinor = 120000,
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = LocalDate.of(2026, 7, 5),
    )

    @Test
    fun `mark paid stamps today and flips status`() = runTest {
        val repo = UpsertingTxRepo(pending())
        MarkTransactionPaidUseCase(repo, clock)("tx-1")

        val result = repo.getById("tx-1")!!
        assertEquals(TransactionStatus.PAID, result.status)
        assertEquals(LocalDate.of(2026, 7, 15), result.actualDate)
    }

    @Test
    fun `unmark reverts to pending and clears the settled date`() = runTest {
        val repo = UpsertingTxRepo(pending())
        MarkTransactionPaidUseCase(repo, clock)("tx-1")
        UnmarkTransactionPaidUseCase(repo, clock)("tx-1")

        val result = repo.getById("tx-1")!!
        assertEquals(TransactionStatus.PENDING, result.status)
        assertNull(result.actualDate)
    }

    @Test
    fun `missing transaction is a no-op`() = runTest {
        val repo = UpsertingTxRepo(pending())
        MarkTransactionPaidUseCase(repo, clock)("does-not-exist")

        assertEquals(TransactionStatus.PENDING, repo.getById("tx-1")!!.status)
    }
}
