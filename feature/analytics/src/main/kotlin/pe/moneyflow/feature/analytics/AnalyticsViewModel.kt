package pe.moneyflow.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.AnalyticsData
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.MonthlyReport
import pe.moneyflow.core.domain.usecase.ExportTransactionsCsvUseCase
import pe.moneyflow.core.domain.usecase.GetAnalyticsUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import pe.moneyflow.core.domain.usecase.GetMonthlyReportUseCase
import java.time.YearMonth
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val analytics: AnalyticsData = AnalyticsData.Empty,
    val report: MonthlyReport = MonthlyReport.empty(YearMonth.now()),
    /** The budget the user overran the most — what the screen opens with, when there is one. */
    val worstOverrun: BudgetProgress? = null,
    /**
     * The budget closest to its limit without crossing it. Only read when [worstOverrun] is null:
     * "nothing is over" is true but not useful on its own, and this is the thing worth watching.
     * Null when no budgets exist at all, which is a different message.
     */
    val closestToLimit: BudgetProgress? = null,
    val insights: List<Insight> = emptyList(),
    val isExporting: Boolean = false,
)

/** One-shot effects the screen consumes (share sheet, snackbars). */
sealed interface AnalyticsEvent {
    data class ShareCsv(val csv: String, val fileName: String) : AnalyticsEvent
    /** Carries the report so the screen (which has a Context) can render + share the PDF. */
    data class SharePdf(val report: MonthlyReport, val fileName: String) : AnalyticsEvent
    data class ExportFailed(val message: String) : AnalyticsEvent
    data object NothingToExport : AnalyticsEvent
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    getAnalytics: GetAnalyticsUseCase,
    getMonthlyReport: GetMonthlyReportUseCase,
    getBudgetsProgress: GetBudgetsProgressUseCase,
    getInsights: GetInsightsUseCase,
    private val exportCsv: ExportTransactionsCsvUseCase,
) : ViewModel() {

    private val exporting = kotlinx.coroutines.flow.MutableStateFlow(false)

    private val events = Channel<AnalyticsEvent>(Channel.BUFFERED)
    val eventFlow: Flow<AnalyticsEvent> = events.receiveAsFlow()

    // Analytics absorbed Sugerencias: an insight is a number with an action behind it, which is
    // exactly what this screen is for — two separate places to look was one too many.
    val uiState: StateFlow<AnalyticsUiState> = combine(
        combine(getAnalytics(), getMonthlyReport()) { a, r -> a to r },
        getBudgetsProgress(),
        getInsights(),
        exporting,
    ) { (analytics, report), budgets, insights, isExporting ->
        AnalyticsUiState(
            isLoading = false,
            analytics = analytics,
            report = report,
            worstOverrun = budgets
                .filter { it.isOverBudget }
                .maxByOrNull { it.spentMinor - it.budget.amountMinor },
            closestToLimit = budgets
                .filterNot { it.isOverBudget }
                .maxByOrNull { it.fraction },
            insights = insights,
            isExporting = isExporting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(isLoading = true),
    )

    /** Builds the current month's CSV and hands it to the screen to write + share. */
    fun exportCurrentMonth() {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            try {
                val csv = exportCsv()
                if (csv.lineSequence().count { it.isNotBlank() } <= 1) {
                    events.send(AnalyticsEvent.NothingToExport)
                } else {
                    val month = YearMonth.now()
                    events.send(
                        AnalyticsEvent.ShareCsv(
                            csv = csv,
                            fileName = "moneyflow-$month.csv",
                        ),
                    )
                }
            } catch (e: Exception) {
                events.send(AnalyticsEvent.ExportFailed(e.message ?: "Error al exportar"))
            } finally {
                exporting.value = false
            }
        }
    }

    /** Hands the current monthly report to the screen to render as a PDF and share. */
    fun exportPdfReport() {
        val report = uiState.value.report
        val hasData = report.currentExpenseMinor > 0 ||
            report.currentIncomeMinor > 0 ||
            report.transactionCount > 0
        viewModelScope.launch {
            if (!hasData) {
                events.send(AnalyticsEvent.NothingToExport)
            } else {
                events.send(
                    AnalyticsEvent.SharePdf(
                        report = report,
                        fileName = "moneyflow-reporte-${report.month}.pdf",
                    ),
                )
            }
        }
    }
}
