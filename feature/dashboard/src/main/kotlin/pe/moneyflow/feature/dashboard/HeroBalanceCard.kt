package pe.moneyflow.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.GlassCard
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.SpendingPace
import pe.moneyflow.core.ui.component.AnimatedAmount
import pe.moneyflow.core.ui.util.toMonthNameOnly
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The dashboard hero.
 *
 * The old version showed one number — "Gastado este mes" and an amount — which cannot answer the
 * question people actually open a spending app to ask: *am I OK?* A figure with no reference is data,
 * not insight. `S/ 1,240` is unreadable without knowing the budget, the usual, or the rate.
 *
 * So the figure now carries three references, in one glance:
 *
 *  - a **denominator** — "de S/ 2,000" plus a progress bar, so the number has a scale;
 *  - a **projection** — where today's rate lands by month end, which is the only forward-looking
 *    number in the app and the one that lets a user change course while it still matters;
 *  - a **comparison** — signed delta against last month, so "a lot" has a baseline.
 *
 * Expense-first is kept deliberately: for an expense tracker, "what have I spent" beats "what's my
 * balance". Income stays secondary.
 *
 * Every row below the amount is always rendered when its data exists, and the card's structure does
 * not change shape between users — the previous version hid income and balance behind
 * `if (monthIncome > 0)`, so the card silently changed height and no landmark below it was stable.
 */
@Composable
fun HeroBalanceCard(
    data: DashboardData,
    pace: SpendingPace?,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        Text(
            text = if (pace == null) {
                "Gastado en ${data.month.toMonthNameOnly()}"
            } else {
                "Gastado este mes"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The headline. Counts to its value so logging an expense visibly moves it.
        AnimatedAmount(
            amountMinor = data.monthSpentMinor,
            currencyCode = data.currencyCode,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // ---- Denominator: the budget this spend is measured against -------------------------------
        val budgetMinor = pace?.monthBudgetMinor
        if (budgetMinor != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "de ${Money.format(budgetMinor, data.currencyCode)} presupuestado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))
            HeroProgressBar(pace = pace, currencyCode = data.currencyCode)
        }

        // ---- Projection: where this month is heading ---------------------------------------------
        if (pace != null && !pace.isComplete) {
            Spacer(Modifier.height(Spacing.md))
            HeroPaceRow(pace = pace, currencyCode = data.currencyCode)
        }

        // ---- Comparison + income ----------------------------------------------------------------
        Spacer(Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            HeroStat(
                label = "vs mes pasado",
                content = { MonthDeltaLabel(data) },
                modifier = Modifier.weight(1f),
            )
            HeroStat(
                label = "Balance",
                content = {
                    Text(
                        text = Money.format(data.balanceMinor, data.currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (data.balanceMinor < 0) MaterialTheme.moneyColors.negative
                        else MaterialTheme.colorScheme.onSurface,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Budget fill for the hero.
 *
 * Not the shared `MoneyProgressBar`: this one sits on the tinted hero surface rather than a card, and
 * it needs a **marker at the "on pace" point** so the bar shows two things at once — where you are, and
 * where you should be by today if the budget were spread evenly. A bar past the marker means you're
 * ahead of schedule even when still under the limit, which is the earliest possible warning.
 */
@Composable
private fun HeroProgressBar(pace: SpendingPace, currencyCode: String) {
    val fraction = (pace.budgetFraction ?: 0f).coerceIn(0f, 1f)
    val expectedFraction = (pace.elapsedDays.toFloat() / pace.daysInMonth).coerceIn(0f, 1f)
    val barColor = when {
        pace.isOverBudget -> MaterialTheme.moneyColors.negative
        pace.isProjectedOverBudget -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column {
        HeroBar(
            fraction = fraction,
            expectedFraction = expectedFraction,
            barColor = barColor,
            trackColor = trackColor,
            markerColor = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${((pace.budgetFraction ?: 0f) * 100).roundToInt()}% usado",
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
            )
            val remaining = pace.remainingBudgetMinor
            if (remaining != null) {
                Text(
                    text = if (remaining >= 0) {
                        "Quedan ${Money.format(remaining, currencyCode)}"
                    } else {
                        "Excedido por ${Money.format(abs(remaining), currencyCode)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Signed month-over-month change. Up is bad here: this is spending, not income. */
@Composable
private fun MonthDeltaLabel(data: DashboardData) {
    val delta = data.spendDeltaMinor
    if (data.previousMonthSpentMinor == 0L) {
        Text(
            text = "Sin datos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    if (delta == 0L) {
        Text(
            text = "Igual",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        return
    }
    val spentMore = delta > 0
    val color = if (spentMore) MaterialTheme.moneyColors.negative
    else MaterialTheme.moneyColors.positive
    val percent = data.spendDeltaFraction?.let { "${abs(it * 100).roundToInt()}%" }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (spentMore) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(IconSize.sm),
        )
        Spacer(Modifier.size(Spacing.xxs))
        Text(
            text = percent ?: Money.format(abs(delta), data.currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/**
 * "At this rate…" — the projection line.
 *
 * Phrased as a rate rather than a verdict. With a budget it also gives the daily allowance, which turns
 * an abstract limit into a decision the user can actually make today.
 */
@Composable
private fun HeroPaceRow(pace: SpendingPace, currencyCode: String) {
    val allowance = pace.remainingDailyAllowanceMinor
    val projectionColor = when {
        pace.isOverBudget || pace.isProjectedOverBudget -> MaterialTheme.moneyColors.negative
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column {
        Text(
            text = "A este ritmo: ${Money.format(pace.projectedMonthEndMinor, currencyCode)} " +
                "al cierre del mes",
            style = MaterialTheme.typography.bodyMedium,
            color = projectionColor,
        )
        if (allowance != null) {
            Text(
                text = "Puedes gastar ${Money.format(allowance, currencyCode)} por día " +
                    "(${pace.daysRemaining} ${if (pace.daysRemaining == 1) "día" else "días"} restantes)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (pace.committedRemainingMinor > 0) {
            Text(
                text = "Ya comprometido: " +
                    Money.format(pace.committedRemainingMinor, currencyCode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Rounded track + fill + an "expected by today" tick, drawn with layout rather than Canvas. */
@Composable
private fun HeroBar(
    fraction: Float,
    expectedFraction: Float,
    barColor: Color,
    trackColor: Color,
    markerColor: Color,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = Motion.progress(),
        label = "hero-budget-fill",
    )
    // BoxWithConstraints so the marker can be placed at a true fraction of the measured width; there
    // is no fractional-offset modifier, and hardcoding a dp would drift across screen sizes.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction)
                .clip(RoundedCornerShape(50))
                .background(barColor),
        )
        // The pace marker. Only meaningful mid-month; at the very edges it just reads as noise.
        if (expectedFraction in 0.02f..0.98f) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * expectedFraction)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(markerColor),
            )
        }
    }
}
