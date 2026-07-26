package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CreateTransferUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `creates a single transfer transaction linking both accounts`() = runTest {
        val repo = RecordingTxRepo()
        val result = CreateTransferUseCase(repo, clock)(
            fromAccountId = "A",
            toAccountId = "B",
            amountMinor = 2_500,
            currencyCode = "PEN",
        )

        assertTrue(result.isSuccess)
        assertEquals(1, repo.saved.size)
        val tx = repo.saved.single()
        assertEquals(TransactionType.TRANSFER, tx.type)
        assertEquals(TransactionStatus.PAID, tx.status)
        assertEquals("A", tx.accountId)
        assertEquals("B", tx.transferAccountId)
        assertEquals(2_500L, tx.amountMinor)
        assertEquals(LocalDate.of(2026, 7, 15), tx.actualDate)
    }

    @Test
    fun `rejects transfer to the same account`() = runTest {
        val repo = RecordingTxRepo()
        val result = CreateTransferUseCase(repo, clock)("A", "A", 1_000, "PEN")
        assertTrue(result.isFailure)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `rejects non-positive amounts`() = runTest {
        val repo = RecordingTxRepo()
        val result = CreateTransferUseCase(repo, clock)("A", "B", 0, "PEN")
        assertTrue(result.isFailure)
        assertTrue(repo.saved.isEmpty())
    }
}
