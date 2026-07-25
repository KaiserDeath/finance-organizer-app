package pe.moneyflow.feature.savings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SavingsRoute

fun NavGraphBuilder.savingsScreen(onBack: () -> Unit) {
    composable<SavingsRoute> {
        SavingsScreen(onBack = onBack)
    }
}
