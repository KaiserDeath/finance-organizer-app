package pe.moneyflow.feature.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

fun NavGraphBuilder.dashboardScreen(
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onOpenUpcoming: () -> Unit,
) {
    composable<DashboardRoute> {
        DashboardScreen(
            onSeeAllTransactions = onSeeAllTransactions,
            onTransactionClick = onTransactionClick,
            onOpenUpcoming = onOpenUpcoming,
        )
    }
}
