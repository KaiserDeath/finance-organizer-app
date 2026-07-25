package pe.moneyflow.feature.paymentmethods

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PaymentMethodsRoute

fun NavGraphBuilder.paymentMethodsScreen(onBack: () -> Unit) {
    composable<PaymentMethodsRoute> {
        PaymentMethodsScreen(onBack = onBack)
    }
}
