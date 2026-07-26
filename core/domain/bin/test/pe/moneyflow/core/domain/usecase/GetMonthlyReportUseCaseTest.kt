package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class GetMonthlyReportUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    private val categories = listOf(
        Category(id = "c1", name = "Comida", iconKey = "food", colorHex = "#FF7043", type = CategoryType.EXPENSE),
    )

    private val transactions = listOf(
        Transaction(id = "1", title = "Almuerzo", amountMinor = 1000, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 15)),
        Transaction(id = "2", title = "Café", amountMinor = 500, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 10)),
        Transaction(id = "3", title = "Mercado", amountMinor = 2000, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 6, 5)),
        Transaction(id = "4", title = "Sueldo", amountMinor = 300000, type = TransactionType.INCOME, actualDate = LocalDate.of(2026, 7, 1)),
    )

    @Test
    fun `compares current month against previous`() = runTest {
        val report = GetMonthlyReportUseCase(
            transactionRepository = FakeTxRepo(transactions),
            categoryRepository = FakeCatRepo(categories),
            settingsRepository = FakeSettings(),
            clock = clock,
        )().first()

        assertEquals(YearMonth.of(2026, 7), report.month)
        assertEquals(1500L, report.currentExpenseMinor)
        assertEquals(2000L, report.previousExpenseMinor)
        assertEquals(-500L, report.expenseDeltaMinor)
        assertEquals(300000L, report.currentIncomeMinor)
        assertEquals(298500L, report.balanceMinor)

        val delta = report.categoryDeltas.first { it.category.id == "c1" }
        assertEquals(1500L, delta.currentMinor)
        assertEquals(2000L, delta.previousMinor)
        assertEquals(-500L, delta.deltaMinor)
    }
}
