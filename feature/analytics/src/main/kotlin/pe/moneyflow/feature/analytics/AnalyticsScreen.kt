package pe.moneyflow.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.ShimmerBox
import pe.moneyflow.core.designsystem.component.StatTile
import pe.moneyflow.core.designsystem.theme.CategoryPalette
import pe.moneyflow.core.designsystem.theme.NegativeRed
import pe.moneyflow.core.designsystem.theme.PositiveGreen
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.AnalyticsData
import pe.moneyflow.core.domain.model.CategoryDelta
import pe.moneyflow.core.domain.model.MonthlyReport
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AnalyticsEvent.ShareCsv -> runShare(context) { CsvExporter.share(context, event.csv, event.fileName) }
                is AnalyticsEvent.SharePdf -> runShare(context) { PdfReportExporter.share(context, event.report, event.fileName) }
                is AnalyticsEvent.ExportFailed ->
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                AnalyticsEvent.NothingToExport ->
                    android.widget.Toast.makeText(
                        context,
                        "No hay movimientos para exportar este mes",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    AnalyticsContent(
        state = uiState,
        onBack = onBack,
        onExportCsv = viewModel::exportCurrentMonth,
        onExportPdf = viewModel::exportPdfReport,
        modifier = modifier,
    )
}

/** Runs a share action, surfacing any file/intent failure as a toast instead of crashing. */
private inline fun runShare(context: android.content.Context, block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            e.message ?: "No se pudo compartir el archivo",
            android.widget.Toast.LENGTH_LONG,
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState,
    onBack: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Análisis y reportes") },
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
        item {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tendencias") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reporte") },
                )
            }
        }

        if (state.isLoading) {
            item { LoadingSkeleton() }
            return@LazyColumn
        }

        if (selectedTab == 0) {
            trendsTab(state.analytics)
        } else {
            reportTab(
                report = state.report,
                isExporting = state.isExporting,
                onExportCsv = onExportCsv,
                onExportPdf = onExportPdf,
            )
        }
    }
    }
}

// ---------------------------------------------------------------------------------------------
// Trends tab
// ---------------------------------------------------------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.trendsTab(data: AnalyticsData) {
    val hasData = data.totalExpenseMinor > 0 || data.totalIncomeMinor > 0
    if (!hasData) {
        item {
            MoneyCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = "Aún no hay datos",
                    subtitle = "Registra gastos para ver tus tendencias de los últimos meses.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    item { SummaryStatRow(data) }
    item { MonthlyTrendCard(data) }
    if (data.categoryBreakdown.isNotEmpty()) {
        item { CategoryBreakdownCard(data) }
    }
    item { WeekdayCard(data) }
}

@Composable
private fun SummaryStatRow(data: AnalyticsData) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        StatTile(
            label = "Gasto total",
            value = Money.format(data.totalExpenseMinor, data.currencyCode),
            icon = Icons.Rounded.Payments,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Promedio diario",
            value = Money.format(data.avgDailyExpenseMinor, data.currencyCode),
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
        BarChart(
            entries = data.months.map { point ->
                BarChartEntry(
                    label = point.month.shortMonthLabel(),
                    value = point.expenseMinor,
                    highlighted = point.month == lastMonth,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryBreakdownCard(data: AnalyticsData) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Por categoría", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val slices = data.categoryBreakdown.mapIndexed { index, spend ->
                DonutSlice(
                    fraction = spend.fraction,
                    color = colorFromHex(
                        spend.category.colorHex,
                        CategoryPalette[index % CategoryPalette.size],
                    ),
                )
            }
            DonutChart(
                slices = slices,
                diameter = 132.dp,
                centerContent = {
                    Text(
                        text = "${data.categoryBreakdown.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                        amount = Money.format(spend.amountMinor, data.currencyCode),
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
        BarChart(
            entries = data.weekdays.map { day ->
                BarChartEntry(
                    label = day.dayOfWeek.shortLabel(),
                    value = day.amountMinor,
                )
            },
            barColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
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
            text = Money.format(report.currentExpenseMinor, report.currencyCode),
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
                value = Money.format(report.currentIncomeMinor, report.currencyCode),
                valueColor = PositiveGreen,
            )
            MiniStat(
                label = "Balance",
                value = Money.format(report.balanceMinor, report.currencyCode),
                valueColor = if (report.balanceMinor >= 0) PositiveGreen else NegativeRed,
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
    val color = if (isBad) NegativeRed else PositiveGreen
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
            text = "${Money.format(abs(deltaMinor), currencyCode)}$percentText vs mes pasado",
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
                text = "Antes ${Money.format(delta.previousMinor, currencyCode)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(delta.currentMinor, currencyCode),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val d = delta.deltaMinor
            if (d != 0L) {
                Text(
                    text = (if (d > 0) "+" else "-") + Money.format(abs(d), currencyCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (d > 0) NegativeRed else PositiveGreen,
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
