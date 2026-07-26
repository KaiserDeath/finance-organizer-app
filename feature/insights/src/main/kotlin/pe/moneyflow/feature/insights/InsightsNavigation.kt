package pe.moneyflow.feature.insights

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object InsightsRoute

fun NavGraphBuilder.insightsScreen(onBack: () -> Unit) {
    composable<InsightsRoute> {
        InsightsScreen(onBack = onBack)
    }
}
