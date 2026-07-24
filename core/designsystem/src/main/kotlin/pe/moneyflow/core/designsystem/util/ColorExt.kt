package pe.moneyflow.core.designsystem.util

import androidx.compose.ui.graphics.Color

/** Parses "#RRGGBB"/"#AARRGGBB" into a Compose [Color], or null when malformed. */
fun String?.toColorOrNull(): Color? {
    if (this.isNullOrBlank()) return null
    return runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrNull()
}

/** Parses a hex color, returning [fallback] when the string is null or invalid. */
fun colorFromHex(hex: String?, fallback: Color): Color = hex.toColorOrNull() ?: fallback
