package pe.moneyflow.core.ui.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val spanish = Locale("es")
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", spanish)
private val fullFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", spanish)

/**
 * "24 Jul" style short label.
 *
 * The `titlecase` here is load-bearing: this used to be `replaceFirstChar { it }`, which returns the
 * character unchanged, so the intended capitalization never happened and dates rendered "24 jul".
 */
fun LocalDate.toShortLabel(): String =
    format(dayMonthFormatter).replaceFirstChar { it.titlecase(spanish) }

fun LocalDate.toFullLabel(): String = format(fullFormatter)

/** Relative day label ("Hoy", "Ayer", "Mañana") falling back to the short date. */
fun LocalDate.toRelativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Hoy"
    today.minusDays(1) -> "Ayer"
    today.plusDays(1) -> "Mañana"
    else -> toShortLabel()
}

/**
 * Due-date phrase relative to [today]: "vence hoy" / "vence mañana" / "vence en N días"
 * once it's in the past "venció hace N días". Used to foreshadow how a pending charge will read.
 */
fun LocalDate.toDueRelativeLabel(today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, this)
    return when {
        days == 0L -> "vence hoy"
        days == 1L -> "vence mañana"
        days > 1L -> "vence en $days días"
        days == -1L -> "venció hace 1 día"
        else -> "venció hace ${-days} días"
    }
}

/** Capitalized month + year, e.g. "Julio 2026". */
fun LocalDate.toMonthTitle(): String = YearMonth.from(this).toMonthTitle()

/** Capitalized month + year, e.g. "Julio 2026". */
fun YearMonth.toMonthTitle(): String {
    val label = month.getDisplayName(TextStyle.FULL, spanish)
        .replaceFirstChar { it.titlecase(spanish) }
    return "$label $year"
}

/** Just the month name, e.g. "Julio" — for phrases that already carry the year elsewhere. */
fun YearMonth.toMonthNameOnly(): String =
    month.getDisplayName(TextStyle.FULL, spanish).replaceFirstChar { it.titlecase(spanish) }
