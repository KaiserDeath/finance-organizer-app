package pe.moneyflow.feature.analytics

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.domain.model.MonthlyReport
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Renders a [MonthlyReport] to a single-page A4 PDF and shares it via the app's [FileProvider].
 * Drawing lives here (not in the ViewModel) because it needs an Android [Context]/Canvas.
 */
object PdfReportExporter {

    private const val EXPORT_DIR = "exports"

    // A4 at 72 dpi, in points.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f

    private val INK = Color.rgb(20, 24, 33)
    private val MUTED = Color.rgb(120, 128, 140)
    private val ACCENT = Color.rgb(37, 99, 235)
    private val UP_BAD = Color.rgb(220, 38, 38)
    private val DOWN_GOOD = Color.rgb(22, 163, 74)

    fun share(context: Context, report: MonthlyReport, fileName: String) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val file = File(dir, fileName)

        val document = PdfDocument()
        try {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create(),
            )
            drawReport(page.canvas, report)
            document.finishPage(page)
            file.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Exportar reporte")
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(chooser)
    }

    private fun drawReport(canvas: android.graphics.Canvas, report: MonthlyReport) {
        val currency = report.currencyCode
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val h2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED
            textSize = 11f
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 13f
        }
        val bodyRight = Paint(body).apply { textAlign = Paint.Align.RIGHT }
        val rule = Paint().apply { color = Color.rgb(226, 229, 234); strokeWidth = 1f }

        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        var y = MARGIN + 24f

        canvas.drawText("MoneyFlow", left, y, title)
        y += 20f
        canvas.drawText("Reporte mensual · ${report.month.fullLabel()}", left, y, label)
        y += 16f
        canvas.drawLine(left, y, right, y, rule)
        y += 30f

        // --- Resumen ---------------------------------------------------------------------------
        canvas.drawText("Resumen", left, y, h2)
        y += 22f

        y = summaryRow(canvas, "Gastado este mes", Money.format(report.currentExpenseMinor, currency), left, right, y, label, bodyRight)
        y = summaryRow(canvas, "Ingresos", Money.format(report.currentIncomeMinor, currency), left, right, y, label, bodyRight)
        y = summaryRow(canvas, "Balance", Money.format(report.balanceMinor, currency), left, right, y, label, bodyRight)
        y = summaryRow(canvas, "Movimientos", report.transactionCount.toString(), left, right, y, label, bodyRight)

        // Variation vs previous month.
        val delta = report.expenseDeltaMinor
        val variationValue = when {
            delta == 0L -> "Igual que el mes pasado"
            else -> {
                val sign = if (delta > 0) "+" else "-"
                val pct = report.expenseDeltaFraction?.let { " (${(abs(it) * 100).toInt()}%)" }.orEmpty()
                "$sign${Money.format(abs(delta), currency)}$pct"
            }
        }
        val variationPaint = Paint(bodyRight).apply {
            color = when {
                delta > 0L -> UP_BAD
                delta < 0L -> DOWN_GOOD
                else -> MUTED
            }
        }
        y = summaryRow(canvas, "Variación vs mes anterior", variationValue, left, right, y, label, variationPaint)

        y += 16f
        canvas.drawLine(left, y, right, y, rule)
        y += 30f

        // --- Categorías ------------------------------------------------------------------------
        canvas.drawText("Gastos por categoría", left, y, h2)
        y += 22f

        if (report.categoryDeltas.isEmpty()) {
            canvas.drawText("Sin gastos registrados este mes.", left, y, label)
            y += 20f
        } else {
            // Column x-positions.
            val colPrev = left + 300f
            val colCurr = left + 400f
            val header = Paint(label).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val headerR = Paint(header).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Categoría", left, y, header)
            canvas.drawText("Mes anterior", colPrev, y, headerR)
            canvas.drawText("Este mes", colCurr, y, headerR)
            canvas.drawText("Cambio", right, y, headerR)
            y += 8f
            canvas.drawLine(left, y, right, y, rule)
            y += 18f

            report.categoryDeltas.take(20).forEach { d ->
                canvas.drawText(d.category.name.take(34), left, y, body)
                canvas.drawText(Money.format(d.previousMinor, currency), colPrev, y, bodyRight)
                canvas.drawText(Money.format(d.currentMinor, currency), colCurr, y, bodyRight)
                val change = d.deltaMinor
                val changePaint = Paint(bodyRight).apply {
                    color = if (change > 0) UP_BAD else if (change < 0) DOWN_GOOD else MUTED
                }
                val changeText = when {
                    change == 0L -> "—"
                    change > 0L -> "+${Money.format(change, currency)}"
                    else -> "-${Money.format(abs(change), currency)}"
                }
                canvas.drawText(changeText, right, y, changePaint)
                y += 20f
            }
        }

        // --- Footer ----------------------------------------------------------------------------
        val generated = LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es")))
        val footer = Paint(label).apply { textSize = 9f }
        canvas.drawText("Generado por MoneyFlow · $generated", left, PAGE_HEIGHT - MARGIN, footer)
    }

    private fun summaryRow(
        canvas: android.graphics.Canvas,
        labelText: String,
        valueText: String,
        left: Float,
        right: Float,
        y: Float,
        labelPaint: Paint,
        valuePaint: Paint,
    ): Float {
        canvas.drawText(labelText, left, y, labelPaint)
        canvas.drawText(valueText, right, y, valuePaint)
        return y + 22f
    }
}
