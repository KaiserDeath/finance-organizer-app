package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.first
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Renders the transactions in a date range to a CSV string (RFC-4180 style quoting). Pure text
 * only — writing the file and sharing it is the platform layer's job (see the analytics feature).
 */
class ExportTransactionsCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val clock: Clock,
) {
    /** Exports the current calendar month by default. */
    suspend operator fun invoke(
        start: LocalDate = YearMonth.now(clock).atDay(1),
        end: LocalDate = LocalDate.now(clock),
    ): String {
        val transactions = transactionRepository.observeBetween(start, end).first()
        val categories = categoryRepository.observeAll().first().associateBy { it.id }
        val methods = paymentMethodRepository.observeAll().first().associateBy { it.id }

        val rows = transactions.sortedBy { it.effectiveDate ?: LocalDate.MIN }
        return buildString {
            append(HEADER.joinToString(SEPARATOR))
            append(NEWLINE)
            rows.forEach { tx ->
                val fields = listOf(
                    tx.effectiveDate?.toString().orEmpty(),
                    tx.title,
                    tx.categoryId?.let { categories[it]?.name }.orEmpty(),
                    tx.paymentMethodId?.let { methods[it]?.name }.orEmpty(),
                    tx.type.label(),
                    tx.status.label(),
                    Money.formatPlain(tx.amountMinor),
                    tx.currencyCode,
                )
                append(fields.joinToString(SEPARATOR) { escape(it) })
                append(NEWLINE)
            }
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun TransactionType.label(): String = when (this) {
        TransactionType.EXPENSE -> "Gasto"
        TransactionType.INCOME -> "Ingreso"
        TransactionType.TRANSFER -> "Transferencia"
    }

    private fun TransactionStatus.label(): String = when (this) {
        TransactionStatus.PENDING -> "Pendiente"
        TransactionStatus.PAID -> "Pagado"
        TransactionStatus.OVERDUE -> "Vencido"
        TransactionStatus.CANCELLED -> "Cancelado"
    }

    companion object {
        private const val SEPARATOR = ","
        private const val NEWLINE = "\r\n"
        private val HEADER = listOf(
            "Fecha", "Título", "Categoría", "Método de pago", "Tipo", "Estado", "Monto", "Moneda",
        )
    }
}
