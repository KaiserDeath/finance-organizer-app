package pe.moneyflow.feature.upcoming

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object UpcomingRoute

fun NavGraphBuilder.upcomingScreen(
    onPaymentClick: (String) -> Unit,
    onOpenRecurring: () -> Unit,
) {
    composable<UpcomingRoute> {
        UpcomingScreen(onPaymentClick = onPaymentClick, onOpenRecurring = onOpenRecurring)
    }
}
