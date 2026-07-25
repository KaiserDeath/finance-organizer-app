package pe.moneyflow.feature.analytics

import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale("es")

/** "jul" style short month label for a [YearMonth]. */
internal fun YearMonth.shortMonthLabel(): String =
    month.getDisplayName(TextStyle.SHORT, spanish).replaceFirstChar { it.uppercase(spanish) }

/** "Julio 2026" style full label. */
internal fun YearMonth.fullLabel(): String {
    val name = month.getDisplayName(TextStyle.FULL, spanish)
        .replaceFirstChar { it.titlecase(spanish) }
    return "$name $year"
}

/** "L", "M", ... single-letter weekday label. */
internal fun DayOfWeek.shortLabel(): String =
    getDisplayName(TextStyle.SHORT, spanish).take(2).replaceFirstChar { it.uppercase(spanish) }
