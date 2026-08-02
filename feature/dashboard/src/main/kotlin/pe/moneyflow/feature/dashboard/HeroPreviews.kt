package pe.moneyflow.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pe.moneyflow.core.designsystem.preview.MoneyFlowPreviewTheme
import pe.moneyflow.core.designsystem.preview.ThemePreviews
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.SpendingPace
import java.time.LocalDate
import java.time.YearMonth

private val july = YearMonth.of(2026, 7)
private val july10 = LocalDate.of(2026, 7, 10)

private fun previewData(
    month: YearMonth = july,
    spent: Long = 1_240_50,
    income: Long = 3_200_00,
) = DashboardData.empty(month).copy(
    monthSpentMinor = spent,
    monthIncomeMinor = income,
    monthTransactionCount = 24,
)

private fun previewPace(
    spent: Long = 1_240_50,
    budget: Long? = 2_000_00,
    committed: Long = 0,
) = SpendingPace.of(july, july10, spent, budget, committed)

/**
 * The four states worth reviewing together. Read them as a set: the card must stay the same *shape*
 * across all of them, since the old version changed height depending on whether income existed and
 * left no stable landmark below it.
 */
@ThemePreviews
@Composable
private fun HeroBalanceCardStatesPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            // On track: bar behind the pace marker, projection under budget.
            HeroBalanceCard(
                data = previewData(spent = 400_00),
                pace = previewPace(spent = 400_00),
                canGoForward = false,
                onPreviousMonth = {},
                onNextMonth = {},
                streak = emptyList(),
                onOpenBudgets = {},
                isFirstRun = false,
                onToggleAmountsHidden = {},
                modifier = Modifier.fillMaxWidth(),
            )
            // Heading for trouble: still under the limit, but the rate says otherwise (amber).
            HeroBalanceCard(
                data = previewData(spent = 900_00),
                pace = previewPace(spent = 900_00, committed = 300_00),
                canGoForward = false,
                onPreviousMonth = {},
                onNextMonth = {},
                streak = emptyList(),
                onOpenBudgets = {},
                isFirstRun = false,
                onToggleAmountsHidden = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Breached: bar and figures go red, and the copy switches to "Excedido por". */
@ThemePreviews
@Composable
private fun HeroOverBudgetPreview() {
    MoneyFlowPreviewTheme {
        HeroBalanceCard(
            data = previewData(spent = 2_310_00),
            pace = previewPace(spent = 2_310_00),
            canGoForward = false,
            onPreviousMonth = {},
            onNextMonth = {},
            streak = emptyList(),
            onOpenBudgets = {},
            isFirstRun = false,
            onToggleAmountsHidden = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** No budget configured: no denominator, no bar, no allowance — but still a projection. */
@Preview(name = "Hero · no budget", group = "dashboard")
@Composable
private fun HeroNoBudgetPreview() {
    MoneyFlowPreviewTheme {
        HeroBalanceCard(
            data = previewData(),
            pace = previewPace(budget = null),
            canGoForward = false,
            onPreviousMonth = {},
            onNextMonth = {},
            streak = emptyList(),
            onOpenBudgets = {},
            isFirstRun = false,
            onToggleAmountsHidden = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A past month: `pace` is null, so no projection or budget appears. Nothing about a finished month is
 * forward-looking, and budgets are evaluated against the *current* period — showing them here would
 * compare June's spend to July's limits.
 */
@Preview(name = "Hero · past month", group = "dashboard")
@Composable
private fun HeroPastMonthPreview() {
    MoneyFlowPreviewTheme {
        HeroBalanceCard(
            data = previewData(month = YearMonth.of(2026, 6), spent = 1_410_00),
            pace = null,
            canGoForward = false,
            onPreviousMonth = {},
            onNextMonth = {},
            streak = emptyList(),
            onOpenBudgets = {},
            isFirstRun = false,
            onToggleAmountsHidden = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** First month of use: no income recorded yet, so the balance goes negative and takes its red. */
@Preview(name = "Hero · no history", group = "dashboard")
@Composable
private fun HeroNoHistoryPreview() {
    MoneyFlowPreviewTheme {
        HeroBalanceCard(
            data = previewData(spent = 120_00, income = 0),
            pace = previewPace(spent = 120_00, budget = null),
            canGoForward = false,
            onPreviousMonth = {},
            onNextMonth = {},
            streak = emptyList(),
            onOpenBudgets = {},
            isFirstRun = false,
            onToggleAmountsHidden = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Month selector", group = "dashboard")
@Composable
private fun MonthSelectorPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MonthSelector(
                month = july,
                canGoForward = false, // current month: forward arrow is disabled, not merely inert
                onPrevious = {},
                onNext = {},
                modifier = Modifier.fillMaxWidth(),
            )
            MonthSelector(
                month = YearMonth.of(2026, 5),
                canGoForward = true,
                onPrevious = {},
                onNext = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
