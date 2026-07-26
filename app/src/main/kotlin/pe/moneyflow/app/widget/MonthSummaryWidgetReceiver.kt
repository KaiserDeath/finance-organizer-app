package pe.moneyflow.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Broadcast receiver that hosts [MonthSummaryWidget]; declared in the manifest. */
class MonthSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthSummaryWidget()
}
