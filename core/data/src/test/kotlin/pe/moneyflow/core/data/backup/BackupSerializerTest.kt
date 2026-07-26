package pe.moneyflow.core.data.backup

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.ExchangeRate
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate

class BackupSerializerTest {

    private val transaction = Transaction(
        id = "t1",
        title = "Transferencia",
        amountMinor = 12_345,
        currencyCode = "PEN",
        accountId = "a1",
        transferAccountId = "a2",
        type = TransactionType.TRANSFER,
        status = TransactionStatus.PAID,
        estimatedDate = LocalDate.of(2026, 7, 20),
        actualDate = LocalDate.of(2026, 7, 20),
        notes = "prueba",
        createdAt = Instant.ofEpochMilli(1_700_000_000_000),
        updatedAt = Instant.ofEpochMilli(1_700_000_500_000),
    )

    private val account = Account(
        id = "a1", name = "Dólares", type = AccountType.EWALLET, currencyCode = "USD",
        openingBalanceMinor = 50_000, colorHex = "#000", iconKey = "wallet",
        createdAt = Instant.ofEpochMilli(1_699_000_000_000),
    )

    private val rate = ExchangeRate(id = "r1", base = "USD", quote = "PEN", rate = 3.75, asOf = LocalDate.of(2026, 7, 1))

    @Test
    fun `backup survives a JSON round-trip preserving dates and enums`() {
        val original = BackupData(
            transactions = listOf(transaction.toDto()),
            accounts = listOf(account.toDto()),
            exchangeRates = listOf(rate.toDto()),
        )

        val json = backupJson.encodeToString(original)
        val restored = backupJson.decodeFromString<BackupData>(json)

        assertEquals(transaction, restored.transactions.single().toDomain())
        assertEquals(account, restored.accounts.single().toDomain())
        assertEquals(rate, restored.exchangeRates.single().toDomain())
    }

    @Test
    fun `unknown future fields are ignored on import`() {
        val json = """{ "version": 99, "transactions": [], "futureField": true }"""
        val restored = backupJson.decodeFromString<BackupData>(json)
        assertEquals(0, restored.transactions.size)
    }
}
