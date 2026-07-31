package pe.moneyflow.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.ui.util.toRelativeLabel

/** A single transaction line: category avatar, title + meta, and the signed amount. */
@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorFromHex(category?.colorHex, MaterialTheme.colorScheme.primary)
    val icon = iconForKey(category?.iconKey ?: "category")

    // A not-yet-paid movement reads differently: it's money owed, not spent. The shared resolver
    // flags it and, when its due date has passed, escalates to "Vencido".
    val display = paymentDisplayStatus(transaction.status, transaction.effectiveDate)
    val isUnsettled = display != PaymentDisplayStatus.PAID

    val meta = buildString {
        category?.let { append(it.name) }
        transaction.effectiveDate?.let {
            if (isNotEmpty()) append(" · ")
            append(it.toRelativeLabel())
        }
    }

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(icon = icon, accent = accent)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md),
        ) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isUnsettled || meta.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    PaymentStatusPill(display)
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        AmountText(
            amountMinor = transaction.amountMinor,
            currencyCode = transaction.currencyCode,
            type = transaction.type,
            style = MaterialTheme.typography.titleMedium,
            // Mute a not-yet-paid amount so it doesn't read like money already spent.
            color = if (isUnsettled) MaterialTheme.colorScheme.onSurfaceVariant else null,
        )
    }
}
