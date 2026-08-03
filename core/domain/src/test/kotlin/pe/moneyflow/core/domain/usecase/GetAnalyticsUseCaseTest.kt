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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class GetAnalyticsUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC)

    private val categories = listOf(
        Category(id = "c1", name = "Comida", iconKey = "food", colorHex = "#FF7043", type = CategoryType.EXPENSE),
    )

    private val transactions = listOf(
        Transaction(id = "1", title = "Almuerzo", amountMinor = 1000, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 15)),
        Transaction(id = "2", title = "Café", amountMinor = 500, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 10)),
        Transaction(id = "3", title = "Mercado", amountMinor = 2000, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 6, 5)),
        Transaction(id = "4", title = "Sueldo", amountMinor = 300000, type = TransactionType.INCOME, actualDate = LocalDate.of(2026, 7, 1)),
        // Outside the 6-month window (Jan) — must be excluded.
        Transaction(id = "5", title = "Viejo", amountMinor = 9999, categoryId = "c1", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 1, 20)),
    )

    private fun useCase(txs: List<Transaction> = transactions) = GetAnalyticsUseCase(
        transactionRepository = FakeTxRepo(txs),
        categoryRepository = FakeCatRepo(categories),
        settingsRepository = FakeSettings(),
        clock = clock,
    )

    @Test
    fun `window spans six months and excludes older transactions`() = runTest {
        val data = useCase()().first()

        assertEquals(6, data.months.size)
        assertEquals(YearMonth.of(2026, 2), data.months.first().month)
        assertEquals(YearMonth.of(2026, 7), data.months.last().month)
    }

    @Test
    fun `the month total is the month's, not the window's`() = runTest {
        val data = useCase()().first()

        // July holds 1000 + 500; June's 2000 is inside the window but not inside the month. The
        // screen labels this figure "Gasto de julio" and the hero prints the same number, so a
        // window-wide sum here was six months of spending under a one-month label.
        assertEquals(1500L, data.monthExpenseMinor)
        assertEquals(300000L, data.monthIncomeMinor)
        // Still reachable through the trend series, which is the field that does span the window.
        assertEquals(2000L, data.months.first { it.month == YearMonth.of(2026, 6) }.expenseMinor)
    }

    @Test
    fun `monthly points aggregate expenses per month`() = runTest {
        val data = useCase()().first()

        val july = data.months.first { it.month == YearMonth.of(2026, 7) }
        val june = data.months.first { it.month == YearMonth.of(2026, 6) }
        assertEquals(1500L, july.expenseMinor)
        assertEquals(300000L, july.incomeMinor)
        assertEquals(2000L, june.expenseMinor)
    }

    @Test
    fun `weekday breakdown sums by day of week`() = runTest {
        val data = useCase()().first()

        // 2026-07-15 is a Wednesday; 2026-07-10 and 2026-06-05 are Fridays.
        val wednesday = data.weekdays.first { it.dayOfWeek == DayOfWeek.WEDNESDAY }
        val friday = data.weekdays.first { it.dayOfWeek == DayOfWeek.FRIDAY }
        assertEquals(1000L, wednesday.amountMinor)
        assertEquals(2500L, friday.amountMinor)
        assertEquals(2, friday.count)
        assertEquals(7, data.weekdays.size)
    }

    @Test
    fun `category breakdown covers the month, not the window`() = runTest {
        val data = useCase()().first()

        assertEquals(1, data.monthCategoryBreakdown.size)
        assertEquals(1500L, data.monthCategoryBreakdown.first().amountMinor)
        assertEquals(0L, data.monthUncategorizedMinor)
    }

    @Test
    fun `spending no category claims is kept, not dropped`() = runTest {
        val data = useCase(
            transactions + listOf(
                Transaction(id = "6", title = "Sin categoría", amountMinor = 700, type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 12)),
                // Points at a category that no longer exists — the case the breakdown's mapNotNull
                // swallowed silently, which is how the donut centre drifted from the total.
                Transaction(id = "7", title = "Categoría borrada", amountMinor = 300, categoryId = "gone", type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 13)),
            ),
        )().first()

        assertEquals(2500L, data.monthExpenseMinor)
        assertEquals(1500L, data.monthCategoryBreakdown.sumOf { it.amountMinor })
        assertEquals(1000L, data.monthUncategorizedMinor)
    }

    @Test
    fun `the breakdown plus the remainder is exactly the month total`() = runTest {
        val data = useCase(
            transactions + Transaction(id = "6", title = "Suelto", amountMinor = 4321, type = TransactionType.EXPENSE, actualDate = LocalDate.of(2026, 7, 9)),
        )().first()

        // What the donut draws has to add up to the figure at its centre, or the ring is a picture
        // of one number wrapped around a different one.
        assertEquals(
            data.monthExpenseMinor,
            data.monthCategoryBreakdown.sumOf { it.amountMinor } + data.monthUncategorizedMinor,
        )
    }
}
