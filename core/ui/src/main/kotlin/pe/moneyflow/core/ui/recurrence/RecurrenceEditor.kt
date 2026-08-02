package pe.moneyflow.core.ui.recurrence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense

/** The two ways to describe a recurrence in the UI. */
enum class RecurrenceMode { INTERVAL, DAYS_OF_MONTH }

/**
 * Hoisted state for [RecurrenceEditor]. Resolve it into the fields a [RecurringExpense] needs via
 * [frequency], [effectiveInterval] and [days] — that keeps the two scheduling modes (fixed interval
 * vs. specific days of the month) behind one small value type both screens share.
 */
data class RecurrenceValue(
    val mode: RecurrenceMode = RecurrenceMode.INTERVAL,
    val interval: Int = 1,
    val unit: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val daysOfMonth: Set<Int> = emptySet(),
    val monthStride: Int = 1,
) {
    /** A day-of-month schedule needs at least one day chosen; interval mode is always complete. */
    val isValid: Boolean
        get() = mode == RecurrenceMode.INTERVAL || daysOfMonth.isNotEmpty()

    val frequency: RecurrenceFrequency
        get() = if (mode == RecurrenceMode.INTERVAL) unit else RecurrenceFrequency.MONTHLY

    val effectiveInterval: Int
        get() = (if (mode == RecurrenceMode.INTERVAL) interval else monthStride).coerceAtLeast(1)

    val days: List<Int>
        get() = if (mode == RecurrenceMode.INTERVAL) emptyList() else daysOfMonth.sorted()
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceEditor(
    value: RecurrenceValue,
    onValueChange: (RecurrenceValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                RecurrenceMode.INTERVAL to "Cada N",
                RecurrenceMode.DAYS_OF_MONTH to "Días del mes",
            )
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = value.mode == mode,
                    onClick = { onValueChange(value.copy(mode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label) }
            }
        }

        when (value.mode) {
            RecurrenceMode.INTERVAL -> {
                NumberRow(
                    leading = "Cada",
                    number = value.interval,
                    onNumberChange = { onValueChange(value.copy(interval = it)) },
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val units = listOf(
                        RecurrenceFrequency.DAILY to "Días",
                        RecurrenceFrequency.WEEKLY to "Semanas",
                        RecurrenceFrequency.MONTHLY to "Meses",
                        RecurrenceFrequency.YEARLY to "Años",
                    )
                    units.forEachIndexed { index, (unit, label) ->
                        SegmentedButton(
                            selected = value.unit == unit,
                            onClick = { onValueChange(value.copy(unit = unit)) },
                            shape = SegmentedButtonDefaults.itemShape(index, units.size),
                        ) { Text(label) }
                    }
                }
            }

            RecurrenceMode.DAYS_OF_MONTH -> {
                OutlinedButton(
                    onClick = {
                        onValueChange(
                            value.copy(daysOfMonth = setOf(15, RecurringExpense.LAST_DAY_OF_MONTH)),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Quincenal (15 y fin de mes)") }

                Text("Elige uno o más días", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    // Days 1–28 exist in every month; 29–31 are omitted to avoid ambiguity in short
                    // months — "Fin de mes" covers the true last day (28/29/30/31).
                    (1..28).forEach { day ->
                        FilterChip(
                            selected = day in value.daysOfMonth,
                            onClick = { onValueChange(value.copy(daysOfMonth = value.daysOfMonth.toggle(day))) },
                            label = { Text(day.toString()) },
                        )
                    }
                    FilterChip(
                        selected = RecurringExpense.LAST_DAY_OF_MONTH in value.daysOfMonth,
                        onClick = {
                            onValueChange(
                                value.copy(
                                    daysOfMonth = value.daysOfMonth.toggle(RecurringExpense.LAST_DAY_OF_MONTH),
                                ),
                            )
                        },
                        label = { Text("Fin de mes") },
                    )
                }

                NumberRow(
                    leading = "Cada",
                    number = value.monthStride,
                    onNumberChange = { onValueChange(value.copy(monthStride = it)) },
                    trailing = "mes(es)",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberRow(
    leading: String,
    number: Int,
    onNumberChange: (Int) -> Unit,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(leading, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = if (number <= 0) "" else number.toString(),
            onValueChange = { text -> onNumberChange(text.filter(Char::isDigit).take(3).toIntOrNull() ?: 0) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(88.dp),
        )
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun Set<Int>.toggle(value: Int): Set<Int> = if (value in this) this - value else this + value

/** Human-readable summary of a saved recurrence, e.g. "Cada 2 meses" or "Día 15, fin de mes". */
fun recurrenceSummary(
    daysOfMonth: List<Int>,
    frequency: RecurrenceFrequency,
    interval: Int,
): String {
    if (daysOfMonth.isNotEmpty()) {
        val days = daysOfMonth.sorted().joinToString(", ") { day ->
            if (day == RecurringExpense.LAST_DAY_OF_MONTH) "fin de mes" else day.toString()
        }
        val every = if (interval > 1) " (cada $interval meses)" else ""
        return "Día $days$every"
    }
    return when (frequency) {
        RecurrenceFrequency.DAILY -> if (interval > 1) "Cada $interval días" else "Diario"
        RecurrenceFrequency.WEEKLY -> if (interval > 1) "Cada $interval semanas" else "Semanal"
        RecurrenceFrequency.MONTHLY -> if (interval > 1) "Cada $interval meses" else "Mensual"
        RecurrenceFrequency.YEARLY -> if (interval > 1) "Cada $interval años" else "Anual"
    }
}
