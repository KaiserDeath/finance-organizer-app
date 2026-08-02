package pe.moneyflow.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import pe.moneyflow.core.designsystem.component.pressScale
import pe.moneyflow.app.backup.BackupScreen
import pe.moneyflow.app.money.MoneyScreen
import pe.moneyflow.app.security.SecurityScreen
import pe.moneyflow.app.legal.LegalScreen
import pe.moneyflow.app.settings.SettingsScreen
import pe.moneyflow.app.settings.ShortcutsScreen
import pe.moneyflow.feature.accounts.AccountsRoute
import pe.moneyflow.feature.accounts.accountsScreen
import pe.moneyflow.feature.addedit.addEditScreen
import pe.moneyflow.feature.addedit.movementDetailScreen
import pe.moneyflow.feature.addedit.navigateToAddEdit
import pe.moneyflow.feature.addedit.navigateToMovementDetail
import pe.moneyflow.feature.analytics.AnalyticsRoute
import pe.moneyflow.feature.analytics.MonthlyReportRoute
import pe.moneyflow.feature.analytics.analyticsScreen
import pe.moneyflow.feature.analytics.monthlyReportScreen
import pe.moneyflow.feature.currency.CurrencyRoute
import pe.moneyflow.feature.currency.currencyScreen
import pe.moneyflow.feature.budgets.BudgetsRoute
import pe.moneyflow.feature.budgets.budgetsScreen
import pe.moneyflow.feature.categories.CategoriesRoute
import pe.moneyflow.feature.categories.categoriesScreen
import pe.moneyflow.feature.dashboard.DashboardRoute
import pe.moneyflow.feature.dashboard.dashboardScreen
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
data object MoneyRoute

@Serializable
data object SettingsRoute

@Serializable
data object BackupRoute

@Serializable
data object SecurityRoute

@Serializable
data object ShortcutsRoute

@Serializable
data object LegalRoute

internal enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    /** App-bar title. Distinct from [label], which has to stay short enough for the nav bar. */
    val title: String,
) {
    DASHBOARD(DashboardRoute, "Inicio", Icons.Rounded.Home, "Inicio"),
    TRANSACTIONS(
        TransactionsRoute(),
        "Movimientos",
        Icons.AutoMirrored.Rounded.ReceiptLong,
        "Movimientos",
    ),
    // Title is not "Análisis y reportes": the monthly report left this screen for its own
    // destination, so the bar would be promising something the screen no longer holds.
    ANALYTICS(AnalyticsRoute, "Análisis", Icons.Rounded.BarChart, "Análisis"),
    // Budgets left the bar: its headline number now shows on the "Tu dinero" row without entering,
    // which was the argument that promoted it in the first place — and four tabs keep the bar
    // legible on 5-inch screens. It stacks under Tu dinero (and under Inicio's "Ver todo") instead.
    MONEY(MoneyRoute, "Tu dinero", Icons.Rounded.AccountBalanceWallet, "Tu dinero"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyFlowApp(
    startInAddTransaction: Boolean = false,
    shellViewModel: AppShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val overdueCount by shellViewModel.overdueCount.collectAsStateWithLifecycle()

    // Right after onboarding, drop the user straight into logging their first movement.
    LaunchedEffect(Unit) {
        if (startInAddTransaction) navController.navigateToAddEdit()
    }
    val currentDestination = backStackEntry?.destination
    val topLevel = TopLevelDestination.entries
        .firstOrNull { currentDestination.isRouteInHierarchy(it.route) }
    val isTopLevel = topLevel != null

    // One collapsing app bar for all five top-level destinations, owned by the shell.
    //
    // The app previously had no rule here: dashboard, transactions, upcoming and "Más" printed their
    // title as an ordinary scrolling list item while analytics used a small pinned TopAppBar — so a
    // title scrolled away on some tabs and stayed put on others, with nothing a user could learn. It
    // also meant nothing in the app responded to scroll, which is a large part of why it read as dated.
    //
    // MediumTopAppBar + exitUntilCollapsed gives the title real presence at rest and hands the space
    // back to content on scroll. Pushed screens keep their own small pinned bars with a back arrow, so
    // hierarchy stays legible: large title = a place, small title + arrow = somewhere you drilled into.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Navigation adapts to the window instead of always being a bottom bar.
    //
    // On a phone this is exactly the bottom bar it replaces. On a tablet or unfolded foldable it becomes
    // a navigation rail — which is the whole point: the app previously rendered one stretched phone
    // column with a full-width five-item bottom bar and enormous dead side margins, because there was
    // no window-size handling anywhere in the codebase.
    //
    // `NavigationSuiteType.None` on detail screens keeps the existing behaviour of hiding navigation
    // entirely once you've drilled in.
    val navSuiteType = if (isTopLevel) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    } else {
        NavigationSuiteType.None
    }

    NavigationSuiteScaffold(
        layoutType = navSuiteType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                // Overdue payments now surface on the dashboard, so the badge rides "Inicio".
                val badgeCount =
                    if (destination == TopLevelDestination.DASHBOARD) overdueCount else 0
                item(
                    selected = currentDestination.isRouteInHierarchy(destination.route),
                    onClick = { navController.navigateToTopLevel(destination.route) },
                    icon = {
                        if (badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        modifier = Modifier.semantics {
                                            contentDescription =
                                                "$badgeCount pago(s) vencido(s)"
                                        },
                                    ) {
                                        Text(text = if (badgeCount > 9) "9+" else "$badgeCount")
                                    }
                                },
                            ) {
                                Icon(destination.icon, contentDescription = null)
                            }
                        } else {
                            Icon(destination.icon, contentDescription = null)
                        }
                    },
                    label = { Text(destination.label) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Only destinations that actually have a collapsing bar participate in scroll. Inicio must
        // be excluded now that it draws none: exitUntilCollapsed consumes the scroll delta to
        // collapse a bar, and with no bar to collapse it swallowed the gesture outright, leaving
        // the dashboard unable to scroll at all.
        modifier = if (isTopLevel && topLevel != TopLevelDestination.DASHBOARD) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        // Inicio draws its brand band under the status bar, so it must not be inset from the top —
        // it applies `statusBarsPadding()` to the band's *content* instead, which is what puts the
        // colour behind the clock. Every other destination keeps the default.
        contentWindowInsets = if (topLevel == TopLevelDestination.DASHBOARD) {
            WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            // The dashboard has no app bar: its hero header *is* the top of the screen, and it owns
            // the month selector. The bar used to print the month too, which both duplicated the
            // selector right below it and lied — it read `LocalDate.now()`, so stepping back a month
            // left the title on the current one.
            if (topLevel != null && topLevel != TopLevelDestination.DASHBOARD) {
                MediumTopAppBar(
                    title = { Text(topLevel.title) },
                    actions = {
                        // Análisis leads with the overrun card; the monthly report is a place you go
                        // on purpose, so it lives here rather than in a tab competing with it.
                        if (topLevel == TopLevelDestination.ANALYTICS) {
                            IconButton(onClick = { navController.navigate(MonthlyReportRoute) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Assessment,
                                    contentDescription = "Reporte mensual",
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
        floatingActionButton = {
            if (isTopLevel) {
                val fabInteraction = remember { MutableInteractionSource() }
                FloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigateToAddEdit()
                    },
                    interactionSource = fabInteraction,
                    modifier = Modifier.pressScale(fabInteraction),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Agregar movimiento")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding),
            // Every one of the 19 destinations previously used navigation-compose's default
            // cross-fade, so nothing told the user whether they were moving deeper into the app or
            // back out of it. Three cases, all decided here rather than per destination:
            //
            //  - Sibling tabs get a **fade-through**. They are peers, not a stack; sliding between
            //    them would imply a spatial order the bottom bar doesn't have.
            //  - Pushed screens get a **directional slide + fade**, so forward and back read as
            //    opposite motions and depth is legible.
            //  - Sheet-hosting destinations get **nothing** — see [isSheetDestination].
            enterTransition = {
                when {
                    isSheetDestination(targetState) -> EnterTransition.None
                    isPeerSwitch(initialState, targetState) -> fadeThroughIn()
                    else -> slideInForward()
                }
            },
            exitTransition = {
                when {
                    isSheetDestination(targetState) -> ExitTransition.None
                    isPeerSwitch(initialState, targetState) -> fadeThroughOut()
                    else -> slideOutForward()
                }
            },
            popEnterTransition = {
                when {
                    isSheetDestination(initialState) -> EnterTransition.None
                    isPeerSwitch(initialState, targetState) -> fadeThroughIn()
                    else -> slideInBack()
                }
            },
            popExitTransition = {
                when {
                    isSheetDestination(initialState) -> ExitTransition.None
                    isPeerSwitch(initialState, targetState) -> fadeThroughOut()
                    else -> slideOutBack()
                }
            },
        ) {
            dashboardScreen(
                onSeeAllTransactions = { navController.navigateToTopLevel(TransactionsRoute()) },
                onTransactionClick = { id -> navController.navigateToMovementDetail(id) },
                onOpenUpcoming = { navController.navigate(UpcomingRoute) },
                // Plain navigate(): budgets is a stacked destination now, so it gets a back arrow.
                onOpenBudgets = { navController.navigate(BudgetsRoute()) },
                onAddExpense = { navController.navigateToAddEdit() },
                onOpenShortcuts = { navController.navigate(ShortcutsRoute) },
            )
            transactionsScreen(
                onTransactionClick = { id -> navController.navigateToMovementDetail(id) },
            )
            budgetsScreen(onBack = { navController.popBackStack() })
            upcomingScreen(
                onPaymentClick = { id -> navController.navigateToMovementDetail(id) },
                onOpenRecurring = { navController.navigate(RecurringRoute) },
                onBack = { navController.popBackStack() },
            )
            composable<MoneyRoute> {
                MoneyScreen(
                    onOpenBudgets = { navController.navigate(BudgetsRoute()) },
                    onOpenUpcoming = { navController.navigate(UpcomingRoute) },
                    onOpenAccounts = { navController.navigate(AccountsRoute) },
                    onOpenSavings = { navController.navigate(SavingsRoute) },
                    onOpenPaymentMethods = { navController.navigate(PaymentMethodsRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategories = { navController.navigate(CategoriesRoute) },
                    onOpenCurrency = { navController.navigate(CurrencyRoute) },
                    onOpenShortcuts = { navController.navigate(ShortcutsRoute) },
                    onOpenRecurring = { navController.navigate(RecurringRoute) },
                    onOpenBackup = { navController.navigate(BackupRoute) },
                    onOpenSecurity = { navController.navigate(SecurityRoute) },
                    onOpenLegal = { navController.navigate(LegalRoute) },
                )
            }
            accountsScreen(onBack = { navController.popBackStack() })
            savingsScreen(onBack = { navController.popBackStack() })
            categoriesScreen(onBack = { navController.popBackStack() })
            paymentMethodsScreen(onBack = { navController.popBackStack() })
            recurringScreen(
                onBack = { navController.popBackStack() },
                onOpenTransaction = { id -> navController.navigateToMovementDetail(id) },
            )
            analyticsScreen(
                onAdjustBudget = { id -> navController.navigate(BudgetsRoute(editBudgetId = id)) },
                onSeeExpenses = { id -> navController.navigate(TransactionsRoute(categoryId = id)) },
            )
            monthlyReportScreen(onBack = { navController.popBackStack() })
            currencyScreen(onBack = { navController.popBackStack() })
            composable<ShortcutsRoute> { ShortcutsScreen(onBack = { navController.popBackStack() }) }
            composable<BackupRoute> { BackupScreen(onBack = { navController.popBackStack() }) }
            composable<SecurityRoute> { SecurityScreen(onBack = { navController.popBackStack() }) }
            composable<LegalRoute> { LegalScreen(onBack = { navController.popBackStack() }) }
            addEditScreen(onDone = { navController.popBackStack() })
            movementDetailScreen(
                onEditAll = { id -> navController.navigateToAddEdit(id) },
                onDone = { navController.popBackStack() },
            )
        }
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
