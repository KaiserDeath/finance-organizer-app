package pe.moneyflow.feature.analytics

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AnalyticsRoute

fun NavGraphBuilder.analyticsScreen(onBack: (() -> Unit)? = null) {
    composable<AnalyticsRoute> {
        AnalyticsScreen(onBack = onBack)
    }
}
