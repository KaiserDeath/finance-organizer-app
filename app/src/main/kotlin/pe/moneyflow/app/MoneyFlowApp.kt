package pe.moneyflow.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import pe.moneyflow.app.backup.BackupScreen
import pe.moneyflow.app.security.SecurityScreen
import pe.moneyflow.app.settings.AppearanceScreen
import pe.moneyflow.feature.accounts.AccountsRoute
import pe.moneyflow.feature.accounts.accountsScreen
import pe.moneyflow.feature.addedit.addEditScreen
import pe.moneyflow.feature.addedit.navigateToAddEdit
import pe.moneyflow.feature.analytics.AnalyticsRoute
import pe.moneyflow.feature.analytics.analyticsScreen
import pe.moneyflow.feature.currency.CurrencyRoute
import pe.moneyflow.feature.currency.currencyScreen
import pe.moneyflow.feature.budgets.BudgetsRoute
import pe.moneyflow.feature.budgets.budgetsScreen
import pe.moneyflow.feature.categories.CategoriesRoute
import pe.moneyflow.feature.categories.categoriesScreen
import pe.moneyflow.feature.dashboard.DashboardRoute
import pe.moneyflow.feature.dashboard.dashboardScreen
import pe.moneyflow.feature.insights.InsightsRoute
import pe.moneyflow.feature.insights.insightsScreen
import pe.moneyflow.feature.paymentmethods.PaymentMethodsRoute
import pe.moneyflow.feature.paymentmethods.paymentMethodsScreen
import pe.moneyflow.feature.recurring.RecurringRoute
import pe.moneyflow.feature.recurring.recurringScreen
import pe.moneyflow.feature.savings.SavingsRoute
import pe.moneyflow.feature.savings.savingsScreen
import pe.moneyflow.feature.transactions.TransactionsRoute
import pe.moneyflow.feature.transactions.transactionsScreen
import pe.moneyflow.feature.upcoming.UpcomingRoute
import pe.moneyflow.feature.upcoming.upcomingScreen

@Serializable
data object MoreRoute

@Serializable
data object BackupRoute

@Serializable
data object SecurityRoute

@Serializable
data object AppearanceRoute

private enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD(DashboardRoute, "Inicio", Icons.Rounded.Home),
    TRANSACTIONS(TransactionsRoute, "Movimientos", Icons.Rounded.ReceiptLong),
    ANALYTICS(AnalyticsRoute, "Análisis", Icons.Rounded.BarChart),
    UPCOMING(UpcomingRoute, "Próximos", Icons.Rounded.CalendarMonth),
    MORE(MoreRoute, "Más", Icons.Rounded.Menu),
}

@Composable
fun MoneyFlowApp(startInAddTransaction: Boolean = false) {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Right after onboarding, drop the user straight into logging their first movement.
    LaunchedEffect(Unit) {
        if (startInAddTransaction) navController.navigateToAddEdit()
    }
    val currentDestination = backStackEntry?.destination
    val isTopLevel = TopLevelDestination.entries.any { currentDestination.isRouteInHierarchy(it.route) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination.isRouteInHierarchy(destination.route),
                            onClick = { navController.navigateToTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel) {
                FloatingActionButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigateToAddEdit()
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Agregar movimiento")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            dashboardScreen(
                onSeeAllTransactions = { navController.navigateToTopLevel(TransactionsRoute) },
                onTransactionClick = { id -> navController.navigateToAddEdit(id) },
                onOpenUpcoming = { navController.navigateToTopLevel(UpcomingRoute) },
                onOpenInsights = { navController.navigate(InsightsRoute) },
            )
            transactionsScreen(
                onTransactionClick = { id -> navController.navigateToAddEdit(id) },
            )
            budgetsScreen(onBack = { navController.popBackStack() })
            upcomingScreen(
                onPaymentClick = { id -> navController.navigateToAddEdit(id) },
            )
            composable<MoreRoute> {
                MoreScreen(
                    onOpenInsights = { navController.navigate(InsightsRoute) },
                    onOpenAccounts = { navController.navigate(AccountsRoute) },
                    onOpenSavings = { navController.navigate(SavingsRoute) },
                    onOpenCategories = { navController.navigate(CategoriesRoute) },
                    onOpenPaymentMethods = { navController.navigate(PaymentMethodsRoute) },
                    onOpenRecurring = { navController.navigate(RecurringRoute) },
                    onOpenBudgets = { navController.navigate(BudgetsRoute) },
                    onOpenCurrency = { navController.navigate(CurrencyRoute) },
                    onOpenAppearance = { navController.navigate(AppearanceRoute) },
                    onOpenBackup = { navController.navigate(BackupRoute) },
                    onOpenSecurity = { navController.navigate(SecurityRoute) },
                )
            }
            insightsScreen(onBack = { navController.popBackStack() })
            accountsScreen(onBack = { navController.popBackStack() })
            savingsScreen(onBack = { navController.popBackStack() })
            categoriesScreen(onBack = { navController.popBackStack() })
            paymentMethodsScreen(onBack = { navController.popBackStack() })
            recurringScreen(onBack = { navController.popBackStack() })
            analyticsScreen()
            currencyScreen(onBack = { navController.popBackStack() })
            composable<AppearanceRoute> { AppearanceScreen(onBack = { navController.popBackStack() }) }
            composable<BackupRoute> { BackupScreen(onBack = { navController.popBackStack() }) }
            composable<SecurityRoute> { SecurityScreen(onBack = { navController.popBackStack() }) }
            addEditScreen(onDone = { navController.popBackStack() })
        }
    }
}

private fun NavDestination?.isRouteInHierarchy(route: Any): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

private fun NavController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
