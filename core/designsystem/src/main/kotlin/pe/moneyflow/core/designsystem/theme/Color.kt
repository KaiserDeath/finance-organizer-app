package pe.moneyflow.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette — an indigo primary with an emerald accent, tuned for a premium finance feel.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E0FF),
    onPrimaryContainer = Color(0xFF14134A),
    secondary = Color(0xFF0EA5A5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC7F5EF),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF2A1800),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1A1A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A22),
    surfaceVariant = Color(0xFFECECF3),
    onSurfaceVariant = Color(0xFF4A4A54),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFC9C9D4),
    outlineVariant = Color(0xFFE2E2EA),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFB7B4FF),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE1E0FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF00201C),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFC7F5EF),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF2A1800),
    background = Color(0xFF0F0F14),
    onBackground = Color(0xFFE6E6EC),
    surface = Color(0xFF17171F),
    onSurface = Color(0xFFE6E6EC),
    surfaceVariant = Color(0xFF2A2A34),
    onSurfaceVariant = Color(0xFFC4C4D0),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    outline = Color(0xFF44444F),
    outlineVariant = Color(0xFF2E2E38),
)

/** Semantic colors for money direction, resolved by theme in [pe.moneyflow.core.designsystem.theme.MoneyFlowTheme]. */
val PositiveGreen = Color(0xFF16A34A)
val NegativeRed = Color(0xFFDC2626)

/** Fallback palette used to color category slices/chips when a stored hex is missing. */
val CategoryPalette = listOf(
    Color(0xFF4F46E5), Color(0xFF0EA5A5), Color(0xFFF59E0B), Color(0xFFEC4899),
    Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFEF4444), Color(0xFF3B82F6),
    Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFFA855F7), Color(0xFF84CC16),
)
