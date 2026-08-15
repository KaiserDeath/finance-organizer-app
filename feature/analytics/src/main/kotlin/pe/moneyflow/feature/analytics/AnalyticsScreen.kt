package pe.moneyflow.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import pe.moneyflow.core.ui.safearea.safeArea
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.BarChart
import pe.moneyflow.core.designsystem.component.BarChartEntry
import pe.moneyflow.core.designsystem.component.DonutChart
import pe.moneyflow.core.designsystem.component.DonutSlice
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.illustration.Illustration
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.MoneyProgressBar
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.ShimmerBox
import pe.moneyflow.core.designsystem.component.StatTile
import pe.moneyflow.core.designsystem.theme.CategoryPalette
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.AnalyticsData
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.model.CategoryDelta
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.MonthlyReport
import pe.moneyflow.core.ui.component.InsightCard
import java.time.LocalDate
import kotlin.math.abs
import pe.moneyflow.core.ui.util.amountsHidden
import pe.moneyflow.core.ui.util.money

@Composable
fun AnalyticsScreen(
    onAdjustBudget: (String) -> Unit,
    onSeeExpenses: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // No export handling here: exporting moved to MonthlyReportScreen along with the report itself,
    // and this screen has no action that can raise an AnalyticsEvent.
    AnalyticsContent(
        state = uiState,
        onBack = onBack,
        onAdjustBudget = onAdjustBudget,
        onSeeExpenses = onSeeExpenses,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Runs a share action, surfacing any file/intent failure inline instead of crashing. */
private suspend inline fun runShare(
    snackbarHostState: SnackbarHostState,
    noinline onRetry: (() -> Unit)?,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: Exception) {
        snackbarHostState.showRetryable(
            message = e.message ?: "No se pudo compartir el archivo",
            onRetry = onRetry,
        )
    }
}

/** Shows [message], offering a retry action when there is something meaningful to retry. */
private suspend fun SnackbarHostState.showRetryable(message: String, onRetry: (() -> Unit)?) {
    val result = showSnackbar(
        message = message,
        actionLabel = if (onRetry != null) "Reintentar" else null,
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed) onRetry?.invoke()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState,
    onBack: (() -> Unit)?,
    onAdjustBudget: (String) -> Unit,
    onSeeExpenses: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.safeArea("snackbar_analytics")) },
        // No topBar: as a bottom-nav destination the shell supplies the collapsing app bar. A back
        // arrow only makes sense if this screen is ever pushed, which the graph doesn't currently do.
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Análisis y reportes") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // The screen opens with where you overspent and what to do about it, not with a chart:
        // a number that doesn't lead anywhere is noise formatted as data.
        val worst = state.worstOverrun
        if (!state.isLoading) {
            item(key = "worst-overrun") {
                if (worst != null) {
                    WorstOverrunCard(
                        progress = worst,
                        onAdjustBudget = { onAdjustBudget(worst.budget.id) },
                        onSeeExpenses = worst.budget.categoryId?.let { id -> { onSeeExpenses(id) } },
                    )
                } else {
                    // Says so rather than collapsing. When this slot rendered nothing, the screen
                    // opened on summary tiles and read as though the actionable card had never been
                    // built — the absence of bad news is itself the answer this screen owes you.
                    //
                    // But "nothing is over" alone occupied the most privileged slot on the screen
                    // while offering nothing to do, which is the charge that demoted the card
                    // before it. It leads with whatever is closest to its limit, so the "two taps
                    // to act" rule holds in both states.
                    val closest = state.closestToLimit
                    NoOverrunCard(
                        closest = closest,
                        onAdjustBudget = closest?.let { p -> { onAdjustBudget(p.budget.id) } },
                        onSeeExpenses = closest?.budget?.categoryId?.let { id ->
                            { onSeeExpenses(id) }
                        },
                    )
                }
            }
        }

        if (state.isLoading) {
            item { LoadingSkeleton() }
            return@LazyColumn
        }

        // The monthly report used to be the second half of a `Tendencias | Reporte` tab pair. The
        // tabs were removed: they put a navigation layer above the one card on this screen that is
        // meant to lead, and the prototype has no tab row here. The report is now a stacked
        // destination behind the app bar's action — a place you deliberately go, not a mode this
        // screen sits in half the time.
        trendsTab(state.analytics, state.insights, onSeeExpenses)
    }
    }
}

/**
 * The monthly report, as a destination rather than a tab.
 *
 * Reuses [AnalyticsViewModel] — the report, the export state and the share events all already live
 * there, and a second ViewModel would duplicate the export pipeline to save recomputing figures this
 * screen ignores. The cost is that opening the report also spins up the trends/budgets/insights
 * flows; worth splitting if this screen ever gets heavier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var lastExport by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AnalyticsEvent.ShareCsv -> runShare(snackbarHostState, lastExport) {
                    CsvExporter.share(context, event.csv, event.fileName)
                }

                is AnalyticsEvent.SharePdf -> runShare(snackbarHostState, lastExport) {
                    PdfReportExporter.share(context, event.report, event.fileName)
                }

                is AnalyticsEvent.ExportFailed ->
                    snackbarHostState.showRetryable(event.message, lastExport)

                AnalyticsEvent.NothingToExport ->
                    snackbarHostState.showSnackbar("No hay movimientos para exportar este mes")
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.safeArea("snackbar_monthly_report")) },
        topBar = {
            TopAppBar(
                title = { Text("Reporte mensual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            reportTab(
                report = uiState.report,
                isExporting = uiState.isExporting,
                onExportCsv = {
                    lastExport = viewModel::exportCurrentMonth
                    viewModel.exportCurrentMonth()
                },
                onExportPdf = {
                    lastExport = viewModel::exportPdfReport
                    viewModel.exportPdfReport()
                },
            )
        }
    }
}

/**
 * The worst overrun with its two exits: fix the limit, or look at the spending.
 *
 * The fixed-expense variant keeps a neutral tone and drops the days-left pressure — rent that
 * blew its budget doesn't get cut, so the honest message is "your limit is short", and the only
 * useful action is correcting it.
 */
@Composable
private fun WorstOverrunCard(
    progress: BudgetProgress,
    onAdjustBudget: () -> Unit,
    onSeeExpenses: (() -> Unit)?,
) {
    val isFixed = progress.isFixedExpense
    val overMinor = progress.spentMinor - progress.budget.amountMinor
    val accent = if (isFixed) MaterialTheme.colorScheme.primary else MaterialTheme.moneyColors.negative

    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = progress.category?.name ?: progress.budget.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = if (isFixed) {
                "Tu límite está corto por ${money(overMinor, progress.currencyCode)}"
            } else {
                "Te pasaste por ${money(overMinor, progress.currencyCode)}"
            },
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = if (isFixed) {
                "Es un gasto fijo: no se recorta, se corrige el límite."
            } else {
                val daysLeft = LocalDate.now().let { it.lengthOfMonth() - it.dayOfMonth }
                "Quedan $daysLeft día(s) de este mes."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = onAdjustBudget,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Ajustar el límite") }
            if (onSeeExpenses != null) {
                OutlinedButton(
                    onClick = onSeeExpenses,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Ver esos gastos") }
            }
        }
    }
}

/**
 * The "nothing is over its limit" counterpart to [WorstOverrunCard].
 *
 * Leads with the good news, then hands over the budget closest to its limit and the same two exits
 * [WorstOverrunCard] offers. With no budgets at all there is nothing to watch, so it keeps the
 * plain reassurance.
 */
@Composable
private fun NoOverrunCard(
    closest: BudgetProgress?,
    onAdjustBudget: (() -> Unit)?,
    onSeeExpenses: (() -> Unit)?,
) {
    // Same thresholds as BudgetMiniRow — one bar meaning one thing across the app.
    val barColor = when {
        closest == null -> MaterialTheme.colorScheme.primary
        closest.isNearLimit -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.moneyColors.positive,
                modifier = Modifier.size(IconSize.sm),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "Ningún límite excedido",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.moneyColors.positive,
            )
        }

        if (closest == null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Todas tus categorías están dentro de su presupuesto este mes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@MoneyCard
        }

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "Lo más cerca del límite",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = closest.category?.name ?: closest.budget.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${closest.percentUsed}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = barColor,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        val daysLeft = LocalDate.now().let { it.lengthOfMonth() - it.dayOfMonth }
        Text(
            text = "${money(closest.spentMinor, closest.currencyCode)} de " +
                "${money(closest.budget.amountMinor, closest.currencyCode)} · " +
                "quedan $daysLeft día(s) de este mes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        MoneyProgressBar(fraction = closest.fraction, color = barColor, height = 6.dp)
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (onAdjustBudget != null) {
                Button(
                    onClick = onAdjustBudget,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Ajustar el límite") }
            }
            if (onSeeExpenses != null) {
                OutlinedButton(
                    onClick = onSeeExpenses,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Ver esos gastos") }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Trends tab
// ---------------------------------------------------------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.trendsTab(
    data: AnalyticsData,
    insights: List<Insight>,
    onSeeExpenses: (String) -> Unit,
) {
    val hasData = data.totalExpenseMinor > 0 || data.totalIncomeMinor > 0
    if (!hasData) {
        item {
            MoneyCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    illustration = Illustration.NoBreakdown,
                    title = "Aún no hay datos",
                    subtitle = "Registra gastos para ver tus tendencias de los últimos meses.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    item { SummaryStatRow(data) }
    // Breakdown before the historical charts: the prototype's Análisis leads with the month total and
    // where it went, because that is what a category limit gets adjusted against. The month-over-month
    // cash-flow curve that used to lead here was removed — the user sessions found the comparison
    // against last month irrelevant, the same finding that took the delta out of the hero.
    if (data.categoryBreakdown.isNotEmpty()) {
        item {
            if (data.categoryBreakdown.size == 1) {
                SingleCategorySummaryCard(data.categoryBreakdown.single(), data.currencyCode)
            } else {
                CategoryBreakdownCard(data)
            }
        }
    }
    item { MonthlyTrendCard(data) }
    item { WeekdayCard(data) }
    // Sugerencias landed here from the old Más drawer: an insight is a number with an action
    // behind it, which is this screen's whole premise.
    if (insights.isNotEmpty()) {
        item(key = "insights-header") {
            SectionHeader(title = "Sugerencias", modifier = Modifier.fillMaxWidth())
        }
        insights.forEach { insight ->
            item(key = "insight-${insight.id}") {
                InsightCard(
                    insight = insight,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = insight.categoryId?.let { categoryId ->
                        { onSeeExpenses(categoryId) }
                    },
                )
            }
        }
    }
}

/** A single-category dataset does not need a 100% donut; state the useful conclusion directly. */
@Composable
private fun SingleCategorySummaryCard(
    spend: pe.moneyflow.core.domain.model.CategorySpend,
    currencyCode: String,
) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Por categoría", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Todo tu gasto está en ${spend.category.name}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = money(spend.amountMinor, currencyCode),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Cuando registres otra categoría, aquí podrás comparar cómo se reparte tu gasto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryStatRow(data: AnalyticsData) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        StatTile(
            label = "Gasto total",
            value = money(data.totalExpenseMinor, data.currencyCode),
            icon = Icons.Rounded.Payments,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Promedio diario",
            value = money(data.avgDailyExpenseMinor, data.currencyCode),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MonthlyTrendCard(data: AnalyticsData) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Gasto por mes", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.lg))
        val lastMonth = data.months.lastOrNull()?.month
        // valueLabel is a plain lambda, so the flag is read here in composition and closed over.
        val hidden = amountsHidden()
        BarChart(
            entries = data.months.map { point ->
                BarChartEntry(
                    label = point.month.shortMonthLabel(),
                    value = point.expenseMinor,
                    highlighted = point.month == lastMonth,
                )
            },
            contentDescription = "Gasto por mes.",
            valueLabel = { Money.format(it.value, data.currencyCode, hidden = hidden) },
            modifier = Modifier
                .fillMaxWidth()
                .safeArea("chart_monthly_trend"),
        )
    }
}

@Composable
private fun CategoryBreakdownCard(data: AnalyticsData) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Por categoría", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.lg))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.safeArea("chart_category_breakdown"),
        ) {
            val slices = data.categoryBreakdown.mapIndexed { index, spend ->
                DonutSlice(
                    fraction = spend.fraction,
                    color = colorFromHex(
                        spend.category.colorHex,
                        CategoryPalette[index % CategoryPalette.size],
                    ),
                )
            }
            val breakdownTotalMinor = data.categoryBreakdown.sumOf { it.amountMinor }
            val topSpend = data.categoryBreakdown.firstOrNull()
            DonutChart(
                slices = slices,
                diameter = 132.dp,
                contentDescription = buildString {
                    append("Gasto por categoría, total ")
                    append(money(breakdownTotalMinor, data.currencyCode))
                    append(".")
                    if (topSpend != null) {
                        append(" Mayor: ${topSpend.category.name}, ")
                        append("${(topSpend.fraction * 100).toInt()}%.")
                    }
                    append(" Detalle: ")
                    data.categoryBreakdown.take(5).forEachIndexed { index, spend ->
                        if (index > 0) append("; ")
                        append(spend.category.name)
                        append(" ")
                        append(money(spend.amountMinor, data.currencyCode))
                        append(" (${(spend.fraction * 100).toInt()}%)")
                    }
                },
                centerContent = {
                    // Total spend, matching the dashboard — the count of categories is not the fact
                    // worth the most privileged position in the chart.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = money(breakdownTotalMinor, data.currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (data.categoryBreakdown.size == 1) "1 categoría"
                            else "${data.categoryBreakdown.size} categorías",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
            )
            Spacer(Modifier.size(Spacing.xl))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                data.categoryBreakdown.take(5).forEachIndexed { index, spend ->
                    LegendRow(
                        name = spend.category.name,
                        amount = money(spend.amountMinor, data.currencyCode),
                        percent = spend.fraction,
                        color = colorFromHex(
                            spend.category.colorHex,
                            CategoryPalette[index % CategoryPalette.size],
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(name: String, amount: String, percent: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(percent * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun WeekdayCard(data: AnalyticsData) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Por día de la semana", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.lg))
        val hidden = amountsHidden()
        BarChart(
            entries = data.weekdays.map { day ->
                BarChartEntry(
                    label = day.dayOfWeek.shortLabel(),
                    value = day.amountMinor,
                )
            },
            contentDescription = "Gasto por día de la semana.",
            valueLabel = { Money.format(it.value, data.currencyCode, hidden = hidden) },
            barColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .safeArea("chart_weekday_spend"),
            barHeight = 110.dp,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Report tab
// ---------------------------------------------------------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.reportTab(
    report: MonthlyReport,
    isExporting: Boolean,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
) {
    item { MonthComparisonCard(report) }
    if (report.categoryDeltas.isNotEmpty()) {
        item {
            SectionHeader(
                title = "Cambios por categoría",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            MoneyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.sm),
            ) {
                report.categoryDeltas.take(8).forEach { delta ->
                    CategoryDeltaRow(delta, report.currencyCode)
                }
            }
        }
    }
    item {
        ExportCard(
            isExporting = isExporting,
            onExportCsv = onExportCsv,
            onExportPdf = onExportPdf,
        )
    }
}

@Composable
private fun MonthComparisonCard(report: MonthlyReport) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(
                text = report.month.fullLabel(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Gastado este mes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = money(report.currentExpenseMinor, report.currencyCode),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        DeltaChip(
            deltaMinor = report.expenseDeltaMinor,
            fraction = report.expenseDeltaFraction,
            currencyCode = report.currencyCode,
            // For spending, up is bad.
            invertColor = true,
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            MiniStat(
                label = "Ingresos",
                value = money(report.currentIncomeMinor, report.currencyCode),
                valueColor = MaterialTheme.moneyColors.positive,
            )
            MiniStat(
                label = "Balance",
                value = money(report.balanceMinor, report.currencyCode),
                valueColor = if (report.balanceMinor >= 0) MaterialTheme.moneyColors.positive
                else MaterialTheme.moneyColors.negative,
            )
            MiniStat(
                label = "Movimientos",
                value = report.transactionCount.toString(),
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DeltaChip(
    deltaMinor: Long,
    fraction: Float?,
    currencyCode: String,
    invertColor: Boolean,
) {
    if (deltaMinor == 0L) {
        Text(
            text = "Igual que el mes pasado",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val up = deltaMinor > 0
    val isBad = if (invertColor) up else !up
    val color = if (isBad) MaterialTheme.moneyColors.negative else MaterialTheme.moneyColors.positive
    val percentText = fraction?.let { " (${(abs(it) * 100).toInt()}%)" }.orEmpty()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(Spacing.xxs))
        Text(
            text = "${money(abs(deltaMinor), currencyCode)}$percentText vs mes pasado",
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun CategoryDeltaRow(delta: CategoryDelta, currencyCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = delta.category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "Antes ${money(delta.previousMinor, currencyCode)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = money(delta.currentMinor, currencyCode),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val d = delta.deltaMinor
            if (d != 0L) {
                Text(
                    text = (if (d > 0) "+" else "-") + money(abs(d), currencyCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (d > 0) MaterialTheme.moneyColors.negative
                    else MaterialTheme.moneyColors.positive,
                )
            }
        }
    }
}

@Composable
private fun ExportCard(
    isExporting: Boolean,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Exportar",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "El CSV lista los movimientos del mes (ábrelo en Excel). El PDF es el reporte con la comparativa por categoría.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(
                onClick = onExportCsv,
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("CSV")
                }
            }
            OutlinedButton(
                onClick = onExportPdf,
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.sm))
                Text("PDF")
            }
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(180.dp))
    }
}
