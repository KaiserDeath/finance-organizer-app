package pe.moneyflow.core.domain.usecase

import org.junit.Assert.assertEquals
import pe.moneyflow.core.domain.model.MessagePart
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

class InsightEngineTest {

    private val today = LocalDate.of(2026, 7, 15)

    private val categories = listOf(
        Category(id = "food", name = "Comida", iconKey = "food", colorHex = "#000", type = CategoryType.EXPENSE),
        Category(id = "fun", name = "Ocio", iconKey = "movie", colorHex = "#000", type = CategoryType.EXPENSE),
    )

    private fun expense(id: String, cat: String, amount: Long, date: LocalDate) = Transaction(
        id = id, title = id, amountMinor = amount, categoryId = cat,
        type = TransactionType.EXPENSE, status = TransactionStatus.PAID, actualDate = date,
    )

    private fun income(id: String, amount: Long, date: LocalDate) = Transaction(
        id = id, title = id, amountMinor = amount, type = TransactionType.INCOME,
        status = TransactionStatus.PAID, actualDate = date,
    )

    /** A bill that has not been paid: drives the overdue and upcoming insights. */
    private fun pending(id: String, amount: Long, due: LocalDate) = Transaction(
        id = id, title = id, amountMinor = amount, type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING, estimatedDate = due,
    )

    @Test
    fun `no transactions yields a getting-started insight`() {
        val insights = InsightEngine.generate(emptyList(), categories, today, "PEN")
        assertEquals(1, insights.size)
        assertEquals(InsightKind.GETTING_STARTED, insights.single().kind)
    }

    @Test
    fun `spending spike is detected against last month`() {
        val txs = listOf(
            expense("p1", "food", 10_000, LocalDate.of(2026, 6, 10)), // last month S/100
            expense("t1", "food", 20_000, LocalDate.of(2026, 7, 5)),  // this month S/200 (+100%)
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        val spike = insights.first { it.kind == InsightKind.SPENDING_SPIKE }
        assertEquals(InsightSeverity.WARNING, spike.severity)
        assertTrue(spike.plainMessage.contains("100%"))
        assertTrue(spike.plainMessage.contains("Comida"))
        // The figure is carried as data, not formatted into the sentence. This is what lets the
        // UI mask it; if an amount ever gets inlined as text again, discreet mode leaks it and
        // nothing else would notice.
        assertEquals(
            listOf(MessagePart.Amount(20_000, "PEN")),
            spike.message.filterIsInstance<MessagePart.Amount>(),
        )
    }

    /**
     * Every insight the engine can emit, checked for inlined money.
     *
     * The rule this pins is "the domain does not format amounts". A regex over the rendered text
     * is the only way to state it generally, and here it is the right tool: it is asserting the
     * absence of a format, not trying to parse one back out.
     */
    @Test
    fun `no insight bakes a formatted amount into its text`() {
        val txs = listOf(
            expense("p1", "food", 10_000, LocalDate.of(2026, 6, 10)),
            expense("t1", "food", 20_000, LocalDate.of(2026, 7, 5)),
            expense("t2", "transport", 5_000, LocalDate.of(2026, 7, 6)),
            income("i1", 300_000, LocalDate.of(2026, 7, 1)),
            pending("b1", 12_000, LocalDate.of(2026, 7, 20)),
            pending("b2", 8_000, LocalDate.of(2026, 6, 20)),
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        assertTrue("expected the engine to produce insights", insights.isNotEmpty())

        val currencyLike = Regex("""[^\s\d.,]{1,3}\s\d[\d,]*\.\d{2}""")
        insights.forEach { insight ->
            insight.message.filterIsInstance<MessagePart.Text>().forEach { part ->
                assertNull(
                    "amount formatted into text of '${insight.id}': ${part.value}",
                    currencyLike.find(part.value),
                )
            }
        }
    }

    @Test
    fun `small increases do not trigger a spike`() {
        val txs = listOf(
            expense("p1", "food", 10_000, LocalDate.of(2026, 6, 10)),
            expense("t1", "food", 10_500, LocalDate.of(2026, 7, 5)), // only +5%
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        assertTrue(insights.none { it.kind == InsightKind.SPENDING_SPIKE })
    }

    @Test
    fun `positive cashflow when income exceeds expenses`() {
        val txs = listOf(
            income("i1", 300_000, LocalDate.of(2026, 7, 1)),
            expense("e1", "food", 50_000, LocalDate.of(2026, 7, 3)),
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        val cashflow = insights.first { it.kind == InsightKind.CASHFLOW }
        assertEquals(InsightSeverity.POSITIVE, cashflow.severity)
    }

    @Test
    fun `negative cashflow warns when expenses exceed income`() {
        val txs = listOf(
            income("i1", 50_000, LocalDate.of(2026, 7, 1)),
            expense("e1", "food", 90_000, LocalDate.of(2026, 7, 3)),
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        val cashflow = insights.first { it.kind == InsightKind.CASHFLOW }
        assertEquals(InsightSeverity.WARNING, cashflow.severity)
    }

    @Test
    fun `upcoming and overdue pending bills are surfaced`() {
        val txs = listOf(
            Transaction(id = "u1", title = "Luz", amountMinor = 8_000, type = TransactionType.EXPENSE, status = TransactionStatus.PENDING, estimatedDate = today.plusDays(3)),
            Transaction(id = "o1", title = "Agua", amountMinor = 5_000, type = TransactionType.EXPENSE, status = TransactionStatus.PENDING, estimatedDate = today.minusDays(2)),
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        assertTrue(insights.any { it.kind == InsightKind.UPCOMING_BILLS })
        assertTrue(insights.any { it.kind == InsightKind.OVERDUE_BILLS && it.severity == InsightSeverity.WARNING })
    }

    @Test
    fun `warnings are ordered before informational and positive insights`() {
        val txs = listOf(
            income("i1", 50_000, LocalDate.of(2026, 7, 1)),
            expense("e1", "food", 90_000, LocalDate.of(2026, 7, 3)), // negative cashflow -> WARNING
        )
        val insights = InsightEngine.generate(txs, categories, today, "PEN")
        val severities = insights.map { it.severity.ordinal }
        assertEquals(severities.sorted(), severities)
    }
}
