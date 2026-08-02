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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.core.designsystem.theme.brandSurface
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.SpendingPace
import pe.moneyflow.core.domain.model.StreakDay
import pe.moneyflow.core.ui.component.AnimatedAmount
import pe.moneyflow.core.ui.util.toMonthNameOnly
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The dashboard hero — a full-bleed brand header rather than a card.
 *
 * It answers the question people open a spending app to ask: *am I OK?* A figure with no reference is
 * data, not insight; `S/ 1,240` is unreadable without knowing the budget or the rate. So the headline
 * carries a **denominator** ("de S/ 2,000" plus a bar, giving the number a scale) and a **projection**
 * (where today's rate lands by month end — the only forward-looking number in the app, and the one
 * that lets a user change course while it still matters).
 *
 * There used to be a third reference: a signed delta against last month, "so 'a lot' has a baseline".
 * The user sessions the Propuesta C handoff required before touching this file settled that against
 * it — participants did not use the month-over-month figure and did not find it relevant. It was
 * removed rather than moved: the same sessions rejected the "Cifras del cierre" block the prototype
 * proposed on Análisis, so there was nowhere it earned a place. Don't reintroduce it from the
 * prototype; its absence is a result, not an oversight.
 *
 * **Why a header and not a card.** The screen previously opened with an app bar printing the month,
 * then a month selector repeating it, then an inset card — three stacked bands of chrome before any
 * number. The prototype collapses all of that into one brand-colored block that owns the month
 * selector, the figure, the pace and the streak, so the first thing on screen is the answer. The app
 * bar is gone on this destination for the same reason; see `MoneyFlowApp.kt`.
 *
 * Colors come from `MaterialTheme.brandSurface`, not from `colorScheme` directly. Picking them here
 * is what got dark mode wrong: Material's `primary` is a *light* lavender in dark theme, so the band
 * rendered as a bright block with dark text. Those roles now live in the design system, where the
 * next brand surface inherits the answer instead of re-deriving it.
 *
 * Expense-first is kept deliberately: for an expense tracker, "what have I spent" beats "what's my
 * balance". Income stays secondary.
 */
@Composable
fun HeroBalanceCard(
    data: DashboardData,
    pace: SpendingPace?,
    canGoForward: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    streak: List<StreakDay>,
    modifier: Modifier = Modifier,
) {
    val brand = MaterialTheme.brandSurface
    val onBand = brand.content
    val onBandMuted = brand.mutedContent

    Surface(
        modifier = modifier,
        color = brand.container,
        contentColor = onBand,
        // Only the bottom corners round: the band runs to the top edge, under the status bar.
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
    ) {
        // The band itself runs under the status bar — the shell hands this destination the top inset
        // rather than consuming it — so the padding goes on the *content*, keeping brand colour
        // behind the clock instead of a strip of page background above it.
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    top = Spacing.xs,
                    bottom = Spacing.xl,
                ),
        ) {
            MonthSelector(
                month = data.month,
                canGoForward = canGoForward,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                contentColor = onBand,
                mutedColor = onBandMuted,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = if (pace == null) {
                    "Gastado en ${data.month.toMonthNameOnly()}"
                } else {
                    "Gastado este mes"
                },
                style = MaterialTheme.typography.labelLarge,
                color = onBandMuted,
            )

            // The headline. Counts to its value so logging an expense visibly moves it.
            AnimatedAmount(
                amountMinor = data.monthSpentMinor,
                currencyCode = data.currencyCode,
                style = MaterialTheme.typography.displayMedium,
                color = onBand,
            )

            // ---- Projection: where this month is heading ---------------------------------------
            if (pace != null && !pace.isComplete) {
                Spacer(Modifier.height(Spacing.sm))
                HeroPaceRow(pace = pace, currencyCode = data.currencyCode, mutedColor = onBandMuted)
            }

            // ---- Denominator: the budget this spend is measured against -------------------------
            val budgetMinor = pace?.monthBudgetMinor
            if (budgetMinor != null) {
                Spacer(Modifier.height(Spacing.md))
                HeroProgressBar(pace = pace, currencyCode = data.currencyCode)
            }

            // ---- Balance ------------------------------------------------------------------------
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = onBandMuted,
                )
                Text(
                    text = Money.format(data.balanceMinor, data.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onBand,
                )
            }

            // ---- Streak, inside the band ---------------------------------------------------------
            if (streak.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                HorizontalDivider(color = onBandMuted)
                Spacer(Modifier.height(Spacing.md))
                StreakRow(days = streak, contentColor = onBand, mutedColor = onBandMuted)
            }
        }
    }
}

/**
 * Budget fill for the hero.
 *
 * Not the shared `MoneyProgressBar`: this one sits on the brand band rather than a card, and it needs
 * a **marker at the "on pace" point** so the bar shows two things at once — where you are, and where
 * you should be by today if the budget were spread evenly. A bar past the marker means you're ahead
 * of schedule even when still under the limit, which is the earliest possible warning.
 */
@Composable
private fun HeroProgressBar(pace: SpendingPace, currencyCode: String) {
    val brand = MaterialTheme.brandSurface
    val onBand = brand.content
    val mutedColor = brand.mutedContent
    val fraction = (pace.budgetFraction ?: 0f).coerceIn(0f, 1f)
    val expectedFraction = (pace.elapsedDays.toFloat() / pace.daysInMonth).coerceIn(0f, 1f)
    val alert = brand.alert
    val barColor = if (pace.isOverBudget || pace.isProjectedOverBudget) alert else onBand

    Column {
        Text(
            text = "de ${Money.format(pace.monthBudgetMinor ?: 0L, currencyCode)} presupuestado",
            style = MaterialTheme.typography.bodyMedium,
            color = mutedColor,
        )
        Spacer(Modifier.height(Spacing.sm))
        HeroBar(
            fraction = fraction,
            expectedFraction = expectedFraction,
            barColor = barColor,
            trackColor = brand.track,
            markerColor = onBand,
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
                    color = if (remaining >= 0) mutedColor else alert,
                )
            }
        }
    }
}

/**
 * "A este ritmo…" — the projection line.
 *
 * Phrased as a rate rather than a verdict. With a budget it also gives the daily allowance, which
 * turns an abstract limit into a decision the user can actually make today.
 */
@Composable
private fun HeroPaceRow(pace: SpendingPace, currencyCode: String, mutedColor: Color) {
    val allowance = pace.remainingDailyAllowanceMinor
    Column {
        Text(
            text = "A este ritmo: ${Money.format(pace.projectedMonthEndMinor, currencyCode)} " +
                "al cierre del mes",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (allowance != null) {
            Text(
                text = "Puedes gastar ${Money.format(allowance, currencyCode)} por día " +
                    "(${pace.daysRemaining} ${if (pace.daysRemaining == 1) "día" else "días"} restantes)",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
            )
        }
        // committedRemainingMinor is deliberately absent: the "Por pagar" StatTile below prints the
        // same figure, in amber, beside "Hoy" where the reader is already scanning for numbers.
        // Saying it twice, two cards apart, only made the band longer.
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
