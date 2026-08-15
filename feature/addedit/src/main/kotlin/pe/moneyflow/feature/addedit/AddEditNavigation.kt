package pe.moneyflow.feature.addedit

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** [transactionId] null = create new; non-null = edit existing. */
@Serializable
data class AddEditRoute(val transactionId: String? = null)

fun NavController.navigateToAddEdit(transactionId: String? = null) {
    navigate(AddEditRoute(transactionId))
}

fun NavGraphBuilder.addEditScreen(onDone: () -> Unit, onSaved: () -> Unit = {}) {
    composable<AddEditRoute> {
        AddEditScreen(onDone = onDone, onSaved = onSaved)
    }
}
