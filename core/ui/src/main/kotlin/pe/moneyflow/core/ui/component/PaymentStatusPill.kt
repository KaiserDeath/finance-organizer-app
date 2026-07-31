package pe.moneyflow.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.TransactionStatus
import java.time.LocalDate

/**
 * How a transaction's payment state should read *right now*. Unlike the stored
 * [TransactionStatus], [OVERDUE] is never persisted — it's derived from a still-unpaid
 * charge whose due date has already passed. Kept in one place so the transaction list and
 * the Add/Edit preview can't drift apart.
 */
enum class PaymentDisplayStatus { PAID, PENDING, OVERDUE }

/**
 * Resolves the display state for a transaction. A [TransactionStatus.PENDING] charge escalates
 * to [PaymentDisplayStatus.OVERDUE] only once its [effectiveDate] is *strictly* before [today],
 * so a payment due today still reads as pending. Everything that isn't pending shows as paid.
 */
fun paymentDisplayStatus(
    status: TransactionStatus,
    effectiveDate: LocalDate?,
    today: LocalDate = LocalDate.now(),
): PaymentDisplayStatus = when {
    status != TransactionStatus.PENDING -> PaymentDisplayStatus.PAID
    effectiveDate != null && effectiveDate.isBefore(today) -> PaymentDisplayStatus.OVERDUE
    else -> PaymentDisplayStatus.PENDING
}

/**
 * Compact tonal badge for a transaction's payment state.
 *
 * By default a settled charge needs no badge, so [PaymentDisplayStatus.PAID] renders nothing —
 * the behavior the transaction list and the Add/Edit preview rely on. Pass [showPaid] = true where
 * "Pagado" is meaningful on its own (e.g. a recurring card that has no amount muting to lean on),
 * and the paid state renders a tonal "Pagado" badge instead.
 */
@Composable
fun PaymentStatusPill(
    display: PaymentDisplayStatus,
    modifier: Modifier = Modifier,
    showPaid: Boolean = false,
) {
    val (label, container, content) = when (display) {
        PaymentDisplayStatus.PAID -> if (showPaid) {
            Triple(
                "Pagado",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            return
        }
        PaymentDisplayStatus.PENDING -> Triple(
            "Pendiente",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        PaymentDisplayStatus.OVERDUE -> Triple(
            "Vencido",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    StatusBadge(label = label, container = container, content = content, modifier = modifier)
}

@Composable
private fun StatusBadge(label: String, container: Color, content: Color, modifier: Modifier) {
    Surface(color = container, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
        )
    }
}
