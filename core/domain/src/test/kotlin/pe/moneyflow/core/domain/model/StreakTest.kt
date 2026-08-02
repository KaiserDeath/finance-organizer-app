package pe.moneyflow.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

class StreakTest {

    private val today = LocalDate.of(2026, 7, 29)

    private fun expense(amount: Long, date: LocalDate, id: String = date.toString() + amount) =
        Transaction(
            id = id,
            title = "Gasto",
            amountMinor = amount,
            type = TransactionType.EXPENSE,
            actualDate = date,
        )

    @Test
    fun `seven days oldest first`() {
        val streak = streakOf(emptyList(), monthBudgetMinor = null, today = today)

        assertEquals(7, streak.size)
        assertEquals(LocalDate.of(2026, 7, 23), streak.first().date)
        assertEquals(today, streak.last().date)
    }

    @Test
    fun `no budget degrades to logged only`() {
        val streak = streakOf(
            listOf(expense(1000, today)),
            monthBudgetMinor = null,
            today = today,
        )

        assertTrue(streak.last().logged)
        assertNull(streak.last().withinAllowance)
        assertFalse(streak.first().logged)
    }

    @Test
    fun `allowance is variable per day`() {
        // Budget 3100 over July (31 days) → flat 100/day when nothing was spent before.
        val streak = streakOf(
            listOf(expense(9000, LocalDate.of(2026, 7, 28))),
            monthBudgetMinor = 310000,
            today = today,
        )

        val day28 = streak.first { it.date == LocalDate.of(2026, 7, 28) }
        val day29 = streak.first { it.date == today }
        // 28th: nothing spent before → allowance (310000-0)/4 = 77500 < 9000? No: 9000 <= 77500.
        assertEquals(true, day28.withinAllowance)
        // 29th: 9000 spent before → allowance (310000-9000)/3 ≈ 100333, spent 0 → within.
        assertEquals(true, day29.withinAllowance)
        assertTrue(day28.logged)
        assertFalse(day29.logged)
    }

    @Test
    fun `overspending a day marks it outside the allowance`() {
        // Budget 3100 minor over 31 days; a 200000 spend on the 23rd blows that day's allowance.
        val streak = streakOf(
            listOf(expense(200000, LocalDate.of(2026, 7, 23))),
            monthBudgetMinor = 310000,
            today = today,
        )

        val day23 = streak.first { it.date == LocalDate.of(2026, 7, 23) }
        assertEquals(false, day23.withinAllowance)
    }

    @Test
    fun `pending rows count as logged but not as spend`() {
        val pending = Transaction(
            id = "p1",
            title = "Netflix",
            amountMinor = 500000,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.PENDING,
            estimatedDate = today,
        )
        val streak = streakOf(listOf(pending), monthBudgetMinor = 310000, today = today)

        val last = streak.last()
        assertTrue(last.logged)
        assertEquals(true, last.withinAllowance)
    }
}
