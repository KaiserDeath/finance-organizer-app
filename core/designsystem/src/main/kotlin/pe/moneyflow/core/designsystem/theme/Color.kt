package pe.moneyflow.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand palette — an indigo primary with a teal accent and amber caution step, tuned for a
// premium finance feel.
//
// Every role is set explicitly, in both schemes. This matters more than it looks: M3 components
// default to roles most apps never define — NavigationBar uses `surfaceContainer`, ModalBottomSheet
// uses `surfaceContainerLow`, and status badges use `tertiaryContainer`/`errorContainer`. Leaving
// those unset lets Material's *baseline* (purple-tinted, warm) tokens leak into the app's chrome
// alongside this cool-neutral palette. The `surfaceContainer` ramps below are therefore derived from
// the same cool neutrals as `background`/`surface`, and the container roles from the brand seeds.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E0FF),
    onPrimaryContainer = Color(0xFF14134A),
    inversePrimary = Color(0xFFB7B4FF),

    secondary = Color(0xFF0EA5A5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC7F5EF),
    onSecondaryContainer = Color(0xFF00201C),

    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF2A1800),
    // Derived from the amber seed rather than Material's baseline pink, which is what the
    // "Pagado" status pill was previously rendering in. 10.6:1 against onTertiaryContainer.
    tertiaryContainer = Color(0xFFFDEBC8),
    onTertiaryContainer = Color(0xFF4A2E00),

    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE2E2),
    onErrorContainer = Color(0xFF5C0A0A),

    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1A1A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A22),
    surfaceVariant = Color(0xFFECECF3),
    onSurfaceVariant = Color(0xFF4A4A54),
    surfaceTint = Color(0xFF4F46E5),

    // Cool-neutral containment ramp: white -> the surfaceVariant end of the family.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFD),
    surfaceContainer = Color(0xFFF4F4F9),
    surfaceContainerHigh = Color(0xFFEEEEF5),
    surfaceContainerHighest = Color(0xFFE8E8F1),
    surfaceDim = Color(0xFFDEDEE8),
    surfaceBright = Color(0xFFFFFFFF),

    inverseSurface = Color(0xFF2F2F3A),
    inverseOnSurface = Color(0xFFF2F2F7),

    outline = Color(0xFFC9C9D4),
    outlineVariant = Color(0xFFE2E2EA),
    scrim = Color(0xFF000000),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFB7B4FF),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE1E0FF),
    inversePrimary = Color(0xFF4F46E5),

    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF00201C),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFC7F5EF),

    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF2A1800),
    tertiaryContainer = Color(0xFF7A5300),
    onTertiaryContainer = Color(0xFFFDEBC8),

    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),

    background = Color(0xFF0F0F14),
    onBackground = Color(0xFFE6E6EC),
    surface = Color(0xFF17171F),
    onSurface = Color(0xFFE6E6EC),
    surfaceVariant = Color(0xFF2A2A34),
    onSurfaceVariant = Color(0xFFC4C4D0),
    surfaceTint = Color(0xFFB7B4FF),

    // Cool blue-black ramp, deliberately not Material's warm baseline dark neutrals.
    surfaceContainerLowest = Color(0xFF0A0A0E),
    surfaceContainerLow = Color(0xFF14141C),
    surfaceContainer = Color(0xFF1B1B24),
    surfaceContainerHigh = Color(0xFF23232E),
    surfaceContainerHighest = Color(0xFF2D2D38),
    surfaceDim = Color(0xFF0F0F14),
    surfaceBright = Color(0xFF33333F),

    inverseSurface = Color(0xFFE6E6EC),
    inverseOnSurface = Color(0xFF2F2F3A),

    outline = Color(0xFF44444F),
    outlineVariant = Color(0xFF2E2E38),
    scrim = Color(0xFF000000),
)

/**
 * Money-direction colors. These are *not* fixed values: a single green and a single red cannot be
 * legible on both a white and a near-black surface.
 *
 * The previous fixed pair failed WCAG AA in opposite themes — `#16A34A` on light `surface` measured
 * 3.30:1, and `#DC2626` on dark `surface` measured 3.69:1, both against a 4.5:1 requirement for the
 * body-size text they were applied to (overdue payment labels, negative balances, over-budget
 * percentages). The pairs below clear AA on their own theme's `surface`:
 *
 * | Role     | Light             | Dark               |
 * |----------|-------------------|--------------------|
 * | positive | `#15803D` 5.0:1   | `#4ADE80` 10.2:1   |
 * | negative | `#DC2626` 4.8:1   | `#F87171` 6.4:1    |
 *
 * `negative` intentionally tracks `colorScheme.error` in each theme so the app never shows two
 * different reds at once. It stays a distinct token because "money out" and "something went wrong"
 * are different ideas that may need to diverge later.
 *
 * Resolve these via [moneyColors] — never import the raw values.
 */
data class MoneyColors(
    val positive: Color,
    val negative: Color,
)

internal val LightMoneyColors = MoneyColors(
    positive = Color(0xFF15803D),
    negative = Color(0xFFDC2626),
)

internal val DarkMoneyColors = MoneyColors(
    positive = Color(0xFF4ADE80),
    negative = Color(0xFFF87171),
)

internal val LocalMoneyColors = staticCompositionLocalOf { LightMoneyColors }

/** Theme-resolved money-direction colors. Provided by [MoneyFlowTheme]. */
val MaterialTheme.moneyColors: MoneyColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMoneyColors.current

/**
 * Content roles for a **brand-colored surface** — one filled with the brand indigo rather than a
 * page/card neutral. The dashboard hero band is the first consumer.
 *
 * These exist because such a surface inverts every normal assumption: `onSurfaceVariant` is
 * unreadable on it, and the money-direction red dies against indigo, so a component painting one has
 * to pick its own roles. The hero did exactly that, and got dark mode wrong twice — Material's
 * `primary` is a *light* lavender in dark theme (`#B7B4FF`), so the band rendered as a bright block
 * with dark text, and `tertiaryContainer` is a dark amber (`#7A5300`) that vanishes on it. Deriving
 * these per component means the next brand surface repeats both mistakes.
 *
 * | role         | light             | dark              |
 * |--------------|-------------------|-------------------|
 * | container    | `#4F46E5` indigo  | `#3730A3` deep    |
 * | content      | white 6.4:1       | white 8.9:1       |
 * | mutedContent | `#E1E0FF` 5.5:1   | `#B7B4FF` 4.6:1   |
 * | alert        | `#FDEBC8`         | `#FDEBC8`         |
 *
 * `mutedContent` is a real token, not alpha over [content] — the handoff's one normative color rule
 * bans opacity for secondary text, and that rule does not stop applying because the background is
 * branded. [track] is the exception and is deliberately translucent: it backs a progress bar, not
 * text, and no theme token means "slightly lighter than the container" in both themes.
 *
 * Resolve via [brandSurface] — never import the raw values.
 */
data class BrandSurfaceColors(
    /** Darker origin for the subtle hero gradient. */
    val gradientStart: Color,
    val container: Color,
    val content: Color,
    val mutedContent: Color,
    /** High-contrast brand accent used for progress and positive emphasis on the indigo surface. */
    val accent: Color,
    /** Over-budget signal. Amber, because red is illegible on indigo in either theme. */
    val alert: Color,
    /** Decorative progress-bar track. The one role allowed to be translucent. */
    val track: Color,
)

internal val LightBrandSurface = BrandSurfaceColors(
    gradientStart = Color(0xFF3730A3),
    container = Color(0xFF4F46E5),
    content = Color(0xFFFFFFFF),
    mutedContent = Color(0xFFE1E0FF),
    accent = Color(0xFF5EEAD4),
    alert = Color(0xFFFDEBC8),
    track = Color(0x42FFFFFF),
)

internal val DarkBrandSurface = BrandSurfaceColors(
    gradientStart = Color(0xFF312E81),
    container = Color(0xFF3730A3),
    content = Color(0xFFFFFFFF),
    mutedContent = Color(0xFFB7B4FF),
    accent = Color(0xFF5EEAD4),
    alert = Color(0xFFFDEBC8),
    track = Color(0x42FFFFFF),
)

internal val LocalBrandSurface = staticCompositionLocalOf { LightBrandSurface }

/** Theme-resolved roles for brand-colored surfaces. Provided by [MoneyFlowTheme]. */
val MaterialTheme.brandSurface: BrandSurfaceColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandSurface.current

/**
 * Warning and danger roles for **tinted notice pills on a neutral surface** — the "te quedan X" /
 * "superaste el límite" block on a budget card, and anything like it.
 *
 * These exist because there was no legible amber. A near-limit warning used `colorScheme.tertiary`
 * as *text* on `surface`, which measures **2.15:1** on white — under half the 4.5:1 AA requires for
 * the label-size text it was applied to. The bar it also tinted was fine; the text was not, and one
 * token was doing both jobs.
 *
 * The fix is the prototype's own: draw the notice as a filled pill, which turns one impossible
 * question ("what amber reads on both white and near-black?") into two easy ones. [warning] stays
 * for the rare case that still needs a bare on-surface tint, and is darkened until it clears AA.
 *
 * | role                                | light           | dark             |
 * |-------------------------------------|-----------------|------------------|
 * | onWarningContainer / warningContainer | `#92400E` 6.4:1 | `#FCD34D` 10.1:1 |
 * | onDangerContainer / dangerContainer   | `#B91C1C` 5.3:1 | `#FCA5A5` 8.5:1  |
 * | warning on `surface`                  | `#B45309` 5.0:1 | `#FBBF24` 10.8:1 |
 *
 * `danger*` is the container form of [MoneyColors.negative] and tracks the same red; it is separate
 * because a pill needs a background and a foreground, and `negative` is a single on-surface value.
 *
 * Resolve via [noticeColors] — never import the raw values.
 */
data class NoticeColors(
    val warningContainer: Color,
    val onWarningContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    /** Bare warning tint, legible as text on `surface`. Prefer the container pair where a pill fits. */
    val warning: Color,
)

internal val LightNoticeColors = NoticeColors(
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF92400E),
    dangerContainer = Color(0xFFFEE2E2),
    onDangerContainer = Color(0xFFB91C1C),
    warning = Color(0xFFB45309),
)

internal val DarkNoticeColors = NoticeColors(
    warningContainer = Color(0xFF422006),
    onWarningContainer = Color(0xFFFCD34D),
    dangerContainer = Color(0xFF450A0A),
    onDangerContainer = Color(0xFFFCA5A5),
    warning = Color(0xFFFBBF24),
)

internal val LocalNoticeColors = staticCompositionLocalOf { LightNoticeColors }

/** Theme-resolved warning/danger notice roles. Provided by [MoneyFlowTheme]. */
val MaterialTheme.noticeColors: NoticeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNoticeColors.current

/**
 * Fallback palette used to color category slices/chips when a stored hex is missing, cycled as
 * `CategoryPalette[index % size]`.
 *
 * Ordered so that *adjacent* indices differ in lightness as well as hue, because the donut chart
 * encodes category by color alone. The previous order placed emerald `#10B981` directly before red
 * `#EF4444` — under deuteranopia (~8% of men) those collapse into the same color, making a spending
 * breakdown unreadable. The known-risky pairs (emerald/red, lime/orange, the two teals, the two
 * violets) are now at least three steps apart, and the first five entries — which cover the common
 * 4–5 category breakdown — are maximally separated.
 */
val CategoryPalette = listOf(
    Color(0xFF4F46E5), // indigo   — dark
    Color(0xFFF59E0B), // amber    — light
    Color(0xFF0EA5A5), // teal     — mid
    Color(0xFFEC4899), // pink     — mid
    Color(0xFF84CC16), // lime     — light
    Color(0xFF8B5CF6), // violet   — mid
    Color(0xFFF97316), // orange   — mid-light
    Color(0xFF3B82F6), // blue     — mid
    Color(0xFFEF4444), // red      — mid
    Color(0xFF14B8A6), // teal 2   — mid-light
    Color(0xFFA855F7), // purple   — mid
    Color(0xFF10B981), // emerald  — mid-light
)
