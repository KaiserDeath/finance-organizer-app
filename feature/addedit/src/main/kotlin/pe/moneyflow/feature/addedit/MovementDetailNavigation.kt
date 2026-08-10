package pe.moneyflow.feature.addedit

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
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
    // A detail sheet must be an overlay destination. Registering it as a composable replaces the
    // current NavHost content, leaving the sheet scrim to dim an empty (black) background.
    // DialogNavigator keeps the previous destination composed underneath the sheet.
    dialog<MovementDetailRoute>(
        dialogProperties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        MovementDetailScreen(onEditAll = onEditAll, onDone = onDone)
    }
}
