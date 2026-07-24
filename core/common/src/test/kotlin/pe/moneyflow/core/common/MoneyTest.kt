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
}
