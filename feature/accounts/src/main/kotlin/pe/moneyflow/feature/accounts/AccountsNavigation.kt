package pe.moneyflow.feature.accounts

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object AccountsRoute

fun NavGraphBuilder.accountsScreen(onBack: () -> Unit) {
    composable<AccountsRoute> {
        AccountsScreen(onBack = onBack)
    }
}
