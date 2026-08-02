package pe.moneyflow.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats PEN with symbol and grouping`() {
        assertEquals("S/ 1,234.56", Money.format(123_456))
        assertEquals("S/ 0.00", Money.format(0))
        assertEquals("S/ 5.00", Money.format(500))
    }

    @Test
    fun `parses plain decimals to minor units`() {
        assertEquals(1250L, Money.parseToMinor("12.50"))
        assertEquals(2000L, Money.parseToMinor("20"))
        assertEquals(123_456L, Money.parseToMinor("1,234.56"))
    }

    @Test
    fun `parses input containing a currency symbol`() {
        assertEquals(2000L, Money.parseToMinor("S/ 20"))
    }

    @Test
    fun `rounds half up at the cent`() {
        assertEquals(1235L, Money.parseToMinor("12.345"))
    }

    @Test
    fun `returns null for invalid input`() {
        assertNull(Money.parseToMinor("abc"))
        assertNull(Money.parseToMinor(""))
    }

    // ---- Discreet mode ------------------------------------------------------------------------

    @Test
    fun `the mask keeps the currency symbol and a fixed width`() {
        // Fixed width is the point: a mask that grew with the number would still leak magnitude,
        // which is most of what it exists to withhold.
        assertEquals(Money.mask("PEN"), Money.format(1_000, "PEN", hidden = true))
        assertEquals(Money.mask("PEN"), Money.format(999_999_99, "PEN", hidden = true))
        assertEquals("S/ ••••••", Money.mask("PEN"))
        assertEquals("$ ••••••", Money.mask("USD"))
    }

    @Test
    fun `hidden false formats exactly as before`() {
        assertEquals(Money.format(123_456, "PEN"), Money.format(123_456, "PEN", hidden = false))
    }
}
