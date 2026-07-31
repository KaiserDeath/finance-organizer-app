package pe.moneyflow.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.model.TransactionStatus
import java.time.LocalDate

class PaymentStatusPillTest {

    private val today = LocalDate.of(2026, 7, 27)

    @Test
    fun `pending with a past due date is overdue`() {
        val result = paymentDisplayStatus(TransactionStatus.PENDING, today.minusDays(1), today)
        assertEquals(PaymentDisplayStatus.OVERDUE, result)
    }

    @Test
    fun `pending due today is still pending, not overdue`() {
        val result = paymentDisplayStatus(TransactionStatus.PENDING, today, today)
        assertEquals(PaymentDisplayStatus.PENDING, result)
    }

    @Test
    fun `pending with a future due date is pending`() {
        val result = paymentDisplayStatus(TransactionStatus.PENDING, today.plusDays(5), today)
        assertEquals(PaymentDisplayStatus.PENDING, result)
    }

    @Test
    fun `pending with no date is pending`() {
        val result = paymentDisplayStatus(TransactionStatus.PENDING, null, today)
        assertEquals(PaymentDisplayStatus.PENDING, result)
    }

    @Test
    fun `paid is always paid regardless of date`() {
        val result = paymentDisplayStatus(TransactionStatus.PAID, today.minusDays(30), today)
        assertEquals(PaymentDisplayStatus.PAID, result)
    }
}
