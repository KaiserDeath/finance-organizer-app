package pe.moneyflow.feature.addedit

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Read-first detail sheet for an existing movement. */
@Serializable
data class MovementDetailRoute(val transactionId: String)

fun NavController.navigateToMovementDetail(transactionId: String) {
    navigate(MovementDetailRoute(transactionId))
}

fun NavGraphBuilder.movementDetailScreen(
    onEditAll: (String) -> Unit,
    onDone: () -> Unit,
) {
    composable<MovementDetailRoute> {
        MovementDetailScreen(onEditAll = onEditAll, onDone = onDone)
    }
}
