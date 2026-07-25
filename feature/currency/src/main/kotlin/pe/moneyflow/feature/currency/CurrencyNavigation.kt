package pe.moneyflow.feature.currency

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object CurrencyRoute

fun NavGraphBuilder.currencyScreen(onBack: () -> Unit) {
    composable<CurrencyRoute> {
        CurrencyScreen(onBack = onBack)
    }
}
