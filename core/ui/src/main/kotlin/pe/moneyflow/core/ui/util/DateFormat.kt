package pe.moneyflow.core.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale("es")
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", spanish)
private val fullFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", spanish)

/** "24 jul" style short label. */
fun LocalDate.toShortLabel(): String = format(dayMonthFormatter).replaceFirstChar { it }

fun LocalDate.toFullLabel(): String = format(fullFormatter)

/** Relative day label ("Hoy", "Ayer", "Mañana") falling back to the short date. */
fun LocalDate.toRelativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Hoy"
    today.minusDays(1) -> "Ayer"
    today.plusDays(1) -> "Mañana"
    else -> toShortLabel()
}

/** Capitalized month + year, e.g. "Julio 2026". */
fun LocalDate.toMonthTitle(): String {
    val month = month.getDisplayName(TextStyle.FULL, spanish)
        .replaceFirstChar { it.titlecase(spanish) }
    return "$month $year"
}
