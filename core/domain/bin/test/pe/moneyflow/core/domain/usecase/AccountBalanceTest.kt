package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.ExchangeRate
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

class AccountBalanceTest {

    private val cash = Account(id = "A", name = "Efectivo", type = AccountType.CASH, currencyCode = "PEN", openingBalanceMinor = 10_000, colorHex = "#000", iconKey = "cash")
    private val usd = Account(id = "B", name = "USD", type = AccountType.EWALLET, currencyCode = "USD", openingBalanceMinor = 20_000, colorHex = "#000", iconKey = "wallet")
    private val card = Account(id = "C", name = "Tarjeta", type = AccountType.CREDIT_CARD, currencyCode = "PEN", openingBalanceMinor = 0, colorHex = "#000", iconKey = "card")

    private val today = LocalDate.of(2026, 7, 15)

    private val transactions = listOf(
        Transaction(id = "1", title = "Sueldo", amountMinor = 5_000, accountId = "A", type = TransactionType.INCOME, status = TransactionStatus.PAID, actualDate = today),
        Transaction(id = "2", title = "Almuerzo", amountMinor = 3_000, accountId = "A", type = TransactionType.EXPENSE, status = TransactionStatus.PAID, actualDate = today),
        Transaction(id = "3", title = "Transfer", amountMinor = 2_000, accountId = "A", transferAccountId = "B", type = TransactionType.TRANSFER, status = TransactionStatus.PAID, actualDate = today),
        Transaction(id = "4", title = "Pendiente", amountMinor = 9_999, accountId = "A", type = TransactionType.EXPENSE, status = TransactionStatus.PENDING, estimatedDate = today),
        Transaction(id = "5", title = "Compra tarjeta", amountMinor = 5_000, accountId = "C", type = TransactionType.EXPENSE, status = TransactionStatus.PAID, actualDate = today),
    )

    @Test
    fun `balance folds paid income expense and transfers, ignoring pending`() {
        // 10000 + 5000 - 3000 - 2000 (transfer out) = 10000; pending 9999 ignored.
        assertEquals(10_000L, balanceOf(cash, transactions))
        // 20000 + 2000 (transfer in) = 22000.
        assertEquals(22_000L, balanceOf(usd, transactions))
        // 0 - 5000 = -5000 (owed on the card).
        assertEquals(-5_000L, balanceOf(card, transactions))
    }

    @Test
    fun `net worth converts to base currency and splits assets and liabilities`() = runTest {
        val rates = listOf(
            ExchangeRate(id = "r", base = "USD", quote = "PEN", rate = 3.75, asOf = today),
        )
        val netWorth = GetNetWorthUseCase(
            accountRepository = FakeAccountRepo(listOf(cash, usd, card)),
            transactionRepository = FakeTxRepo(transactions),
            exchangeRateRepository = FakeExchangeRateRepo(rates),
            settingsRepository = FakeSettings(currency = "PEN"),
        )().first()

        // Assets: cash 10000 PEN + usd 22000 * 3.75 = 82500 PEN => 92500.
        assertEquals(92_500L, netWorth.assetsMinor)
        // Liabilities: card -5000 PEN.
        assertEquals(-5_000L, netWorth.liabilitiesMinor)
        assertEquals(87_500L, netWorth.totalMinor)
        assertEquals("PEN", netWorth.currencyCode)
        assertFalse(netWorth.hasUnconvertible)
    }

    @Test
    fun `net worth flags accounts with no conversion rate`() = runTest {
        val jpy = Account(id = "D", name = "Yen", type = AccountType.BANK, currencyCode = "JPY", openingBalanceMinor = 100_000, colorHex = "#000", iconKey = "account_balance")
        val netWorth = GetNetWorthUseCase(
            accountRepository = FakeAccountRepo(listOf(cash, jpy)),
            transactionRepository = FakeTxRepo(emptyList()),
            exchangeRateRepository = FakeExchangeRateRepo(emptyList()),
            settingsRepository = FakeSettings(currency = "PEN"),
        )().first()

        // Only cash (already PEN) contributes; JPY has no rate.
        assertEquals(10_000L, netWorth.totalMinor)
        assertTrue(netWorth.hasUnconvertible)
    }
}
