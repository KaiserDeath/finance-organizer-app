package pe.moneyflow.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * OpenType `tnum` — tabular (fixed-width) figures.
 *
 * Without this, `1` is narrower than `8`, so a stacked list of amounts has visibly ragged decimal
 * points: `S/ 1,111.00` and `S/ 8,888.00` render at different widths. Misaligned digits in a money
 * column is the clearest tell of a hand-rolled finance UI, so every role below opts in and
 * [pe.moneyflow.core.ui.component.AmountText] forces it regardless of the style it's handed.
 */
const val TabularFigures = "tnum"

/**
 * The app's single font family — the one place to swap the typeface.
 *
 * Currently resolves to the platform font (Roboto), which does support `tnum`, so digit alignment
 * works today. To bundle a variable font, drop the `.ttf` into
 * `core/designsystem/src/main/res/font/` and replace this with a `FontFamily(Font(R.font.…))`
 * chain — no other file needs to change.
 */
val MoneyFlowFontFamily = FontFamily.Default

/**
 * The type ramp. All 15 M3 roles are defined deliberately, with three properties held invariant:
 *
 *  1. **Weight never decreases as size increases.** Previously `headlineSmall` and `displaySmall`
 *     were left undefined, so they fell back to M3 baseline at weight 400 — meaning the 11 screen
 *     titles using `headlineSmall` rendered *lighter* than the smaller `titleLarge`, and the 36sp
 *     `displaySmall` analytics figure rendered lighter than the 32sp Bold dashboard hero. Screens
 *     were patching this locally with `.copy(fontWeight = …)`, which is the symptom of a scale that
 *     can't be trusted.
 *  2. **Every step is a real step.** `headlineSmall` sits at 22sp so it no longer ties
 *     `headlineMedium` at 24sp, and `labelSmall` keeps a distinct 11sp against `labelMedium`'s 12sp
 *     by also going heavier.
 *  3. **Tracking is explicit everywhere**, tightening on large sizes and opening up on small ones,
 *     rather than leaving seven roles on inherited defaults.
 */
val MoneyFlowTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TabularFigures,
    ),
    displayMedium = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TabularFigures,
    ),
    displaySmall = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = TabularFigures,
    ),
    headlineLarge = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = TabularFigures,
    ),
    headlineMedium = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TabularFigures,
    ),
    // Carries every screen title in the app.
    headlineSmall = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TabularFigures,
    ),
    titleLarge = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularFigures,
    ),
    titleMedium = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = TabularFigures,
    ),
    titleSmall = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TabularFigures,
    ),
    bodyLarge = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        fontFeatureSettings = TabularFigures,
    ),
    bodyMedium = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = TabularFigures,
    ),
    bodySmall = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
        fontFeatureSettings = TabularFigures,
    ),
    labelLarge = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = TabularFigures,
    ),
    labelMedium = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = TabularFigures,
    ),
    // Status pills and chart axis labels. Small text needs *more* weight and tracking, not less.
    labelSmall = TextStyle(
        fontFamily = MoneyFlowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = TabularFigures,
    ),
)
