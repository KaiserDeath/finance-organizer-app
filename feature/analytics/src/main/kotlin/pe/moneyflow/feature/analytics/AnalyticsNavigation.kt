package pe.moneyflow.feature.analytics

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AnalyticsRoute

/** The monthly report — stacked behind Análisis's app-bar action, not a tab inside it. */
@Serializable
data object MonthlyReportRoute

fun NavGraphBuilder.analyticsScreen(
    onAdjustBudget: (String) -> Unit,
    onSeeExpenses: (String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    composable<AnalyticsRoute> {
        AnalyticsScreen(
            onAdjustBudget = onAdjustBudget,
            onSeeExpenses = onSeeExpenses,
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.monthlyReportScreen(onBack: () -> Unit) {
    composable<MonthlyReportRoute> {
        MonthlyReportScreen(onBack = onBack)
    }
}
