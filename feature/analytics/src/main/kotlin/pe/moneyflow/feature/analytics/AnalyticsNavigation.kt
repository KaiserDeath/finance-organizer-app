package pe.moneyflow.feature.analytics

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AnalyticsRoute

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
