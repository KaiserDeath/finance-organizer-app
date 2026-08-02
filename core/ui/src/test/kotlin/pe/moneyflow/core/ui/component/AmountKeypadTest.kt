package pe.moneyflow.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.common.Money

class AmountKeypadTest {

    @Test
    fun `digits append`() {
        assertEquals("1", apply("1", ""))
        assertEquals("18", apply("8", "1"))
    }

    @Test
    fun `leading zero is replaced, not stacked`() {
        assertEquals("7", apply("7", "0"))
    }

    @Test
    fun `separator on an empty value gets its zero`() {
        assertEquals("0.", apply(".", ""))
    }

    @Test
    fun `only one separator`() {
        assertEquals("1.5", apply(".", "1.5"))
    }

    @Test
    fun `at most two decimals`() {
        assertEquals("1.50", apply("0", "1.5"))
        assertEquals("1.50", apply("9", "1.50"))
    }

    @Test
    fun `backspace removes the last character and stops at empty`() {
        assertEquals("1.5", apply("⌫", "1.50"))
        assertEquals("", apply("⌫", ""))
    }

    @Test
    fun `everything the keypad can produce parses as money`() {
        var value = ""
        listOf("1", "8", ".", "5", "0").forEach { value = apply(it, value) }
        assertEquals("18.50", value)
        assertEquals(1850L, Money.parseToMinor(value))
    }
}
