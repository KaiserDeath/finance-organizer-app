package pe.moneyflow.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.moneyflow.core.model.ExchangeRate
import java.time.LocalDate

class CurrencyConverterTest {

    private val rates = listOf(
        ExchangeRate(id = "1", base = "USD", quote = "PEN", rate = 3.75, asOf = LocalDate.of(2026, 7, 1)),
        ExchangeRate(id = "2", base = "EUR", quote = "PEN", rate = 4.0, asOf = LocalDate.of(2026, 7, 1)),
    )

    @Test
    fun `identity rate is one`() {
        assertEquals(1.0, CurrencyConverter.rate("PEN", "PEN", rates)!!, 0.0)
    }

    @Test
    fun `direct rate is used`() {
        assertEquals(3.75, CurrencyConverter.rate("USD", "PEN", rates)!!, 0.0001)
    }

    @Test
    fun `inverse rate is derived`() {
        assertEquals(1.0 / 3.75, CurrencyConverter.rate("PEN", "USD", rates)!!, 0.0001)
    }

    @Test
    fun `pivot rate crosses two currencies`() {
        // USD -> PEN (3.75) then PEN -> EUR (1/4.0) => 0.9375 EUR per USD.
        assertEquals(3.75 / 4.0, CurrencyConverter.rate("USD", "EUR", rates)!!, 0.0001)
    }

    @Test
    fun `convert rounds to nearest minor unit`() {
        // 100.00 USD -> PEN = 375.00 => 37500 minor units.
        assertEquals(37500L, CurrencyConverter.convert(10000, "USD", "PEN", rates))
    }

    @Test
    fun `unknown currency yields null`() {
        assertNull(CurrencyConverter.rate("JPY", "PEN", rates))
        assertNull(CurrencyConverter.convert(10000, "JPY", "PEN", rates))
    }
}
