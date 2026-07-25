package pe.moneyflow.feature.categories

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object CategoriesRoute

fun NavGraphBuilder.categoriesScreen(onBack: () -> Unit) {
    composable<CategoriesRoute> {
        CategoriesScreen(onBack = onBack)
    }
}
