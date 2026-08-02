package pe.moneyflow.feature.transactions

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class TransactionsRoute(
    /** Pre-applies a category filter — how "Ver esos gastos" in Análisis lands here. */
    val categoryId: String? = null,
)

fun NavGraphBuilder.transactionsScreen(
    onTransactionClick: (String) -> Unit,
) {
    composable<TransactionsRoute> {
        TransactionsScreen(onTransactionClick = onTransactionClick)
    }
}
