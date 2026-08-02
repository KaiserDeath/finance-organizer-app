package pe.moneyflow.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The dashboard hero now makes claims about the future ("a este ritmo…", "puedes gastar X por día"), so
 * the arithmetic behind those claims is worth pinning down. A projection that reads low is worse than
 * no projection: it reassures the user precisely when they should be adjusting.
 */
class SpendingPaceTest {

    private val july = YearMonth.of(2026, 7) // 31 days
    private val july10 = LocalDate.of(2026, 7, 10)

    private fun pace(
        month: YearMonth = july,
        today: LocalDate = july10,
        spentMinor: Long = 0,
        budgetMinor: Long? = null,
        committedMinor: Long = 0,
    ) = SpendingPace.of(month, today, spentMinor, budgetMinor, committedMinor)

    @Test
    fun `elapsed days is the day of month for the current month`() {
        val p = pace(spentMinor = 100_00)
        assertEquals(10, p.elapsedDays)
        assertEquals(31, p.daysInMonth)
        assertEquals(21, p.daysRemaining)
        assertFalse(p.isComplete)
    }

    @Test
    fun `a past month is fully elapsed and has no projection`() {
        val p = pace(month = YearMonth.of(2026, 6), spentMinor = 900_00)
        assertTrue(p.isComplete)
        assertEquals(0, p.daysRemaining)
        // Spent *is* the final number; there is no "at this rate" for a month already over.
        assertEquals(900_00L, p.projectedMonthEndMinor)
        assertNull(p.remainingDailyAllowanceMinor)
    }

    @Test
    fun `daily average divides spend by elapsed days`() {
        val p = pace(spentMinor = 500_00)
        assertEquals(50_00L, p.dailyAverageMinor)
    }

    @Test
    fun `projection extends the run rate across the remaining days`() {
        // 10 days in at S/ 50 per day, 21 days left -> 500 + 1050 = 1550
        val p = pace(spentMinor = 500_00)
        assertEquals(1_550_00L, p.projectedMonthEndMinor)
    }

    @Test
    fun `projection adds committed charges on top of the run rate`() {
        // Rent due on the 28th is not in the first ten days' average, so it must be added rather than
        // assumed — otherwise the projection under-reports for most of the month.
        val p = pace(spentMinor = 500_00, committedMinor = 800_00)
        assertEquals(2_350_00L, p.projectedMonthEndMinor)
    }

    @Test
    fun `budget fraction is uncapped so overspend stays visible`() {
        val p = pace(spentMinor = 1_200_00, budgetMinor = 1_000_00)
        assertEquals(1.2f, p.budgetFraction!!, 0.001f)
        assertTrue(p.isOverBudget)
        // Already over, so it is not *projected* over — that flag is for the not-yet case.
        assertFalse(p.isProjectedOverBudget)
    }

    @Test
    fun `projected over budget flags trouble before the limit is breached`() {
        // 10 days in at S/ 50/day projects to 1550 against a 1200 budget, while spend is still under.
        val p = pace(spentMinor = 500_00, budgetMinor = 1_200_00)
        assertFalse(p.isOverBudget)
        assertTrue(p.isProjectedOverBudget)
    }

    @Test
    fun `remaining daily allowance spreads what is left over the days left`() {
        // 1000 budget, 500 spent, 21 days left -> 500/21 = 23.80...
        val p = pace(spentMinor = 500_00, budgetMinor = 1_000_00)
        assertEquals(23_80L, p.remainingDailyAllowanceMinor)
    }

    @Test
    fun `allowance floors at zero rather than going negative`() {
        // "You may spend -S/ 12 per day" is not a sentence.
        val p = pace(spentMinor = 1_500_00, budgetMinor = 1_000_00)
        assertEquals(0L, p.remainingDailyAllowanceMinor)
    }

    @Test
    fun `no budget means no denominator-derived figures`() {
        val p = pace(spentMinor = 500_00, budgetMinor = null)
        assertNull(p.budgetFraction)
        assertNull(p.remainingBudgetMinor)
        assertNull(p.remainingDailyAllowanceMinor)
        assertFalse(p.isOverBudget)
        assertFalse(p.isProjectedOverBudget)
    }

    @Test
    fun `a zero budget is treated as no budget rather than dividing by zero`() {
        val p = pace(spentMinor = 500_00, budgetMinor = 0)
        assertNull(p.budgetFraction)
    }

    @Test
    fun `day one does not divide by zero`() {
        val p = pace(today = LocalDate.of(2026, 7, 1), spentMinor = 30_00)
        assertEquals(1, p.elapsedDays)
        assertEquals(30_00L, p.dailyAverageMinor)
    }

    @Test
    fun `the last day of the month is complete`() {
        val p = pace(today = LocalDate.of(2026, 7, 31), spentMinor = 900_00)
        assertTrue(p.isComplete)
        assertEquals(900_00L, p.projectedMonthEndMinor)
    }

    @Test
    fun `a future month starts at day one so averages stay finite`() {
        val p = pace(month = YearMonth.of(2026, 12), spentMinor = 0)
        assertEquals(1, p.elapsedDays)
        assertFalse(p.isComplete)
    }
}
