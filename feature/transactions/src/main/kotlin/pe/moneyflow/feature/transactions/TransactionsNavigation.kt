package pe.moneyflow.feature.transactions

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TransactionsRoute

fun NavGraphBuilder.transactionsScreen(
    onTransactionClick: (String) -> Unit,
) {
    composable<TransactionsRoute> {
        TransactionsScreen(onTransactionClick = onTransactionClick)
    }
}
