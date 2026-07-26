package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.PaymentMethodType
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExportTransactionsCsvUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    private val categories = listOf(
        Category(id = "c1", name = "Comida, bebidas", iconKey = "food", colorHex = "#FF7043", type = CategoryType.EXPENSE),
    )
    private val methods = listOf(
        PaymentMethod(id = "p1", name = "Yape", type = PaymentMethodType.EWALLET, iconKey = "yape", colorHex = "#742284"),
    )
    private val transactions = listOf(
        Transaction(id = "1", title = "Almuerzo", amountMinor = 1050, categoryId = "c1", paymentMethodId = "p1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 15)),
        Transaction(id = "2", title = "Café \"grande\"", amountMinor = 500, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 10)),
        // June transaction — outside the default (current month) range.
        Transaction(id = "3", title = "Viejo", amountMinor = 2000, type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 6, 5)),
    )

    private fun useCase() = ExportTransactionsCsvUseCase(
        transactionRepository = FakeTxRepo(transactions),
        categoryRepository = FakeCatRepo(categories),
        paymentMethodRepository = FakePmRepo(methods),
        clock = clock,
    )

    @Test
    fun `exports current month with header and escaped fields`() = runTest {
        val csv = useCase()()
        val lines = csv.trim().split("\r\n")

        // Header + two July rows (June excluded).
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("Fecha,Título,Categoría"))

        // Earliest date (Jul 10) sorts first; its title has quotes that must be doubled.
        assertTrue(lines[1].contains("\"Café \"\"grande\"\"\""))
        assertTrue(lines[1].contains("\"Comida, bebidas\"")) // comma field quoted
        assertTrue(lines[1].contains("5.00"))
        // Jul 15 row.
        assertTrue(lines[2].contains("Almuerzo"))
        assertTrue(lines[2].contains("Yape"))
        assertTrue(lines[2].contains("10.50"))
    }
}
