package pe.moneyflow.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import pe.moneyflow.app.MainActivity
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.domain.model.DashboardData

/**
 * Home-screen widget showing the current month's balance, spend and income. Tapping it opens the
 * app. Data is read once per update from the dashboard use case via [WidgetEntryPoint].
 */
class MonthSummaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val data = entryPoint.getDashboardUseCase().invoke().first()
        // Glance runs outside the app's composition, so LocalAmountsHidden cannot reach it. The
        // flag is read here and passed down — a widget that kept printing the balance would be the
        // worst hole of all, since it is on the home screen with the phone locked to nobody.
        val hidden = entryPoint.settingsRepository().preferences.first().amountsHidden
        provideContent { WidgetContent(data, hidden) }
    }
}

@Composable
private fun WidgetContent(data: DashboardData, hidden: Boolean) {
    val balance = data.monthIncomeMinor - data.monthSpentMinor
    val context = LocalContext.current
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        ) {
            Text(
                text = "Este mes",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = Money.format(balance, data.currencyCode, hidden = hidden),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetricColumn(
                    label = "Gastos",
                    amountMinor = data.monthSpentMinor,
                    currency = data.currencyCode,
                    hidden = hidden,
                )
                Spacer(GlanceModifier.width(20.dp))
                MetricColumn(
                    label = "Ingresos",
                    amountMinor = data.monthIncomeMinor,
                    currency = data.currencyCode,
                    hidden = hidden,
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, amountMinor: Long, currency: String, hidden: Boolean) {
    Column {
        Text(
            text = label,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Text(
            text = Money.format(amountMinor, currency, hidden = hidden),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
