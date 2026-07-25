package pe.moneyflow.feature.budgets

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object BudgetsRoute

fun NavGraphBuilder.budgetsScreen() {
    composable<BudgetsRoute> {
        BudgetsScreen()
    }
}
