package pe.moneyflow.feature.budgets

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class BudgetsRoute(
    /** Opens the editor for this budget on arrival — how "Ajustar el límite" lands here. */
    val editBudgetId: String? = null,
)

fun NavGraphBuilder.budgetsScreen(onBack: () -> Unit) {
    composable<BudgetsRoute> {
        BudgetsScreen(onBack = onBack)
    }
}
