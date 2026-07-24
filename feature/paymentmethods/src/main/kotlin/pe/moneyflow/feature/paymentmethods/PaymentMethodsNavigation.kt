package pe.moneyflow.feature.paymentmethods

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PaymentMethodsRoute

fun NavGraphBuilder.paymentMethodsScreen() {
    composable<PaymentMethodsRoute> {
        PaymentMethodsScreen()
    }
}
