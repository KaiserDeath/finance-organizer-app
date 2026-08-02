package pe.moneyflow.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App theme.
 *
 * **The brand palette is the identity — there is no Material You path.** A finance app's palette is
 * a trust signal, and the Propuesta C redesign commits to it end to end: the prototype, the spec and
 * this design system all assume the brand indigo, and surfaces like [GlassCard] and the dashboard's
 * hero band are built on it. Dynamic color used to be an opt-in switch on the Appearance screen; it
 * was removed because turning it on silently discarded that entire identity — a wallpaper-derived
 * slate replaced the brand, and the app read as though the redesign had never been implemented.
 * Re-adding it means deciding what a brand surface should do when the brand is gone, which is a
 * design question nobody has answered.
 *
 * Runs on material3 1.4.0. Note that `MaterialExpressiveTheme` and `MotionScheme` are *internal* in
 * 1.4.0 — the Expressive theming surface only becomes public in the 1.5.0 alpha line — so motion
 * specs come from our own [Motion] tokens instead. Everything else 1.4.0 offers (medium app bars with
 * scroll behavior, the animated navigation-bar indicator, `PullToRefreshBox`, `PrimaryTabRow`) is in
 * use. Swap [Motion] for `MaterialTheme.motionScheme` when 1.5.0 ships stable.
 *
 * Also provides theme-resolved [MoneyColors] and [BrandSurfaceColors], and keeps the system bar
 * icons legible against the current background.
 */
@Composable
fun MoneyFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val moneyColors = if (darkTheme) DarkMoneyColors else LightMoneyColors
    val brandSurface = if (darkTheme) DarkBrandSurface else LightBrandSurface

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalMoneyColors provides moneyColors,
        LocalBrandSurface provides brandSurface,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoneyFlowTypography,
            shapes = MoneyFlowShapes,
            content = content,
        )
    }
}
