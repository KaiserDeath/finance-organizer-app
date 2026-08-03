package pe.moneyflow.core.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.preview.MoneyFlowPreviewTheme
import pe.moneyflow.core.designsystem.preview.ThemeAndScalePreviews
import pe.moneyflow.core.designsystem.preview.ThemePreviews
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.amount
import pe.moneyflow.core.domain.model.msg
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.component.AmountText
import pe.moneyflow.core.ui.component.ColorSwatchPicker
import pe.moneyflow.core.ui.component.IconChoicePicker
import pe.moneyflow.core.ui.component.InsightCard
import pe.moneyflow.core.ui.component.PaymentDisplayStatus
import pe.moneyflow.core.ui.component.PaymentStatusPill
import pe.moneyflow.core.ui.component.TransactionRow
import java.time.LocalDate

private val previewCategory = Category(
    id = "c1",
    name = "Comida",
    iconKey = "food",
    colorHex = "#F59E0B",
)

private fun previewTransaction(
    id: String,
    title: String,
    amountMinor: Long,
    type: TransactionType = TransactionType.EXPENSE,
    status: TransactionStatus = TransactionStatus.PAID,
) = Transaction(
    id = id,
    title = title,
    amountMinor = amountMinor,
    categoryId = previewCategory.id,
    type = type,
    status = status,
    actualDate = if (status == TransactionStatus.PAID) LocalDate.now() else null,
    estimatedDate = LocalDate.now(),
)

/**
 * Amount alignment and direction color.
 *
 * The decimal points must line up down the column, and both the green and the red must stay legible
 * against the surface in dark mode — the old fixed pair failed WCAG AA in opposite themes.
 */
@ThemeAndScalePreviews
@Composable
private fun AmountTextPreview() {
    MoneyFlowPreviewTheme {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            AmountText(1_111_11, "PEN", TransactionType.EXPENSE, style = MaterialTheme.typography.titleMedium)
            AmountText(8_888_88, "PEN", TransactionType.EXPENSE, style = MaterialTheme.typography.titleMedium)
            AmountText(1_888_11, "PEN", TransactionType.INCOME, style = MaterialTheme.typography.titleMedium)
            AmountText(8_111_88, "PEN", TransactionType.INCOME, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@ThemeAndScalePreviews
@Composable
private fun TransactionRowPreview() {
    MoneyFlowPreviewTheme {
        MoneyCard(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.xs),
        ) {
            TransactionRow(
                transaction = previewTransaction("t1", "Almuerzo", 24_50),
                category = previewCategory,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            TransactionRow(
                transaction = previewTransaction("t2", "Sueldo", 3_200_00, TransactionType.INCOME),
                category = previewCategory,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            TransactionRow(
                transaction = previewTransaction(
                    "t3",
                    "Internet",
                    89_90,
                    status = TransactionStatus.PENDING,
                ),
                category = previewCategory,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "Pagado" must read as amber-derived here, not Material's baseline pink. */
@ThemePreviews
@Composable
private fun PaymentStatusPillPreview() {
    MoneyFlowPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PaymentStatusPill(display = PaymentDisplayStatus.PAID, showPaid = true)
            PaymentStatusPill(display = PaymentDisplayStatus.PENDING)
            PaymentStatusPill(display = PaymentDisplayStatus.OVERDUE)
        }
    }
}

/** All three severities in one place — the dashboard and the insights list now share this mapping. */
@ThemePreviews
@Composable
private fun InsightCardPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            InsightCard(
                insight = Insight(
                    id = "i1",
                    kind = InsightKind.SPENDING_SPIKE,
                    severity = InsightSeverity.WARNING,
                    title = "Gastaste 30% más en Comida",
                    message = msg(amount(42_000, "PEN"), " este mes frente a ", amount(32_300, "PEN"), " el mes pasado."),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            InsightCard(
                insight = Insight(
                    id = "i2",
                    kind = InsightKind.CASHFLOW,
                    severity = InsightSeverity.POSITIVE,
                    title = "Vas mejor que el mes pasado",
                    message = msg("Has gastado ", amount(18_000, "PEN"), " menos hasta la fecha."),
                ),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            InsightCard(
                insight = Insight(
                    id = "i3",
                    kind = InsightKind.UPCOMING_BILLS,
                    severity = InsightSeverity.INFO,
                    title = "3 pagos por vencer",
                    message = msg("Total ", amount(78_000, "PEN"), " en los próximos 7 días."),
                ),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Both pickers keep their compact visual size while every cell is a 48dp touch target. Enable the
 * layout inspector's bounds overlay to confirm the cells, not the dots, are what you tap.
 */
@Preview(name = "Pickers", group = "components")
@Composable
private fun PickersPreview() {
    MoneyFlowPreviewTheme {
        var hex by remember { mutableStateOf("#0EA5A5") }
        var key by remember { mutableStateOf("transport") }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Color", style = MaterialTheme.typography.labelLarge)
            ColorSwatchPicker(selectedHex = hex, onSelect = { hex = it })
            Text("Ícono", style = MaterialTheme.typography.labelLarge)
            IconChoicePicker(
                choices = listOf("food", "transport", "home", "shopping", "health", "movie"),
                selectedKey = key,
                onSelect = { key = it },
            )
        }
    }
}
