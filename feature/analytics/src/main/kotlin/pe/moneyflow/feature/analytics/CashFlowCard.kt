package pe.moneyflow.feature.analytics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.CumulativeLineChart
import pe.moneyflow.core.designsystem.component.LineChartSelection
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.domain.model.CumulativeSpend
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Cash-flow curve: this month's running spend against last month's, at the same points.
 *
 * The readout above the chart is the whole reason the chart is interactive. At rest it shows the
 * month-to-date comparison; while scrubbing it shows the two values on the day under the finger. That
 * turns "a picture of my spending" into something you can interrogate — "when did the gap open up?" —
 * which is the question a static chart leaves you unable to ask.
 */
@Composable
fun CashFlowCard(
    cumulative: CumulativeSpend,
    modifier: Modifier = Modifier,
) {
    var selection by remember { mutableStateOf<LineChartSelection?>(null) }
    val currency = cumulative.currencyCode

    MoneyCard(modifier = modifier) {
        SectionHeader(title = "Ritmo de gasto", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))

        // One slot, two modes — the figures swap in place rather than the card changing height.
        AnimatedContent(
            targetState = selection,
            transitionSpec = {
                fadeIn(Motion.effectsDefault()) togetherWith fadeOut(Motion.effectsDefault())
            },
            label = "cashflow-readout",
        ) { scrubbed ->
            if (scrubbed == null) {
                CashFlowSummary(cumulative)
            } else {
                CashFlowScrubReadout(scrubbed, currency)
            }
        }

        Spacer(Modifier.height(Spacing.md))
        CumulativeLineChart(
            current = cumulative.current,
            previous = cumulative.previous,
            peakMinor = cumulative.peakMinor,
            contentDescription = cumulative.describe(),
            onSelectionChange = { selection = it },
        )
        Spacer(Modifier.height(Spacing.sm))
        CashFlowLegend()
    }
}

@Composable
private fun CashFlowSummary(cumulative: CumulativeSpend) {
    val currency = cumulative.currencyCode
    Column {
        Text(
            text = Money.format(cumulative.currentTotalMinor, currency),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val delta = cumulative.deltaAtSameDayMinor
        if (cumulative.previousAtSameDayMinor == 0L) {
            Text(
                text = "Sin mes anterior para comparar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            DeltaLine(
                deltaMinor = delta,
                baseMinor = cumulative.previousAtSameDayMinor,
                currencyCode = currency,
                suffix = "que el mes pasado a la fecha",
            )
        }
    }
}

@Composable
private fun CashFlowScrubReadout(selection: LineChartSelection, currencyCode: String) {
    Column {
        Text(
            text = "Día ${selection.index + 1}: " +
                Money.format(selection.currentMinor, currencyCode),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val previous = selection.previousMinor
        if (previous == null) {
            Text(
                text = "Sin dato del mes pasado en este día",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            DeltaLine(
                deltaMinor = selection.currentMinor - previous,
                baseMinor = previous,
                currencyCode = currencyCode,
                suffix = "vs ${Money.format(previous, currencyCode)} el mes pasado",
            )
        }
    }
}

/** Signed comparison line. Spending more is bad here, so up is red. */
@Composable
private fun DeltaLine(
    deltaMinor: Long,
    baseMinor: Long,
    currencyCode: String,
    suffix: String,
) {
    if (deltaMinor == 0L) {
        Text(
            text = "Igual $suffix",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val spentMore = deltaMinor > 0
    val color = if (spentMore) MaterialTheme.moneyColors.negative
    else MaterialTheme.moneyColors.positive
    val percent = if (baseMinor > 0) {
        " (${abs(deltaMinor.toFloat() / baseMinor * 100).roundToInt()}%)"
    } else {
        ""
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (spentMore) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(IconSize.sm),
        )
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            text = Money.format(abs(deltaMinor), currencyCode) + percent + " " + suffix,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/** Solid = this month, dashed = last month. The dash is what makes the pair readable without colour. */
@Composable
private fun CashFlowLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendKey("Este mes", MaterialTheme.colorScheme.primary, dashed = false)
        LegendKey("Mes pasado", MaterialTheme.colorScheme.onSurfaceVariant, dashed = true)
    }
}

@Composable
private fun LegendKey(label: String, color: Color, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.55f)),
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .width(16.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Screen-reader description: the shape of the comparison, not a list of 31 numbers. */
private fun CumulativeSpend.describe(): String {
    if (!hasData) return "Ritmo de gasto. Sin datos todavía."
    val direction = when {
        deltaAtSameDayMinor > 0 -> "por encima"
        deltaAtSameDayMinor < 0 -> "por debajo"
        else -> "igual"
    }
    return "Ritmo de gasto acumulado. " +
        "Este mes ${Money.format(currentTotalMinor, currencyCode)} al día ${current.size}, " +
        "$direction del mes pasado " +
        "(${Money.format(previousAtSameDayMinor, currencyCode)}) en el mismo día."
}
