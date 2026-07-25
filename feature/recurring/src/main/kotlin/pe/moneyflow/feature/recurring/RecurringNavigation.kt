package pe.moneyflow.feature.recurring

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object RecurringRoute

fun NavGraphBuilder.recurringScreen(
    onBack: () -> Unit,
) {
    composable<RecurringRoute> {
        RecurringScreen(onBack = onBack)
    }
}
