package pe.moneyflow.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App theme.
 *
 * The brand palette is the default identity: [dynamicColor] is strictly opt-in, because a finance
 * app's palette is a trust signal and shouldn't be delegated to whatever the user's wallpaper
 * happens to be. Material You remains available for users who prefer it — see the Appearance
 * screen — but it is off unless explicitly turned on.
 *
 * Runs on material3 1.4.0. Note that `MaterialExpressiveTheme` and `MotionScheme` are *internal* in
 * 1.4.0 — the Expressive theming surface only becomes public in the 1.5.0 alpha line — so motion
 * specs come from our own [Motion] tokens instead. Everything else 1.4.0 offers (medium app bars with
 * scroll behavior, the animated navigation-bar indicator, `PullToRefreshBox`, `PrimaryTabRow`) is in
 * use. Swap [Motion] for `MaterialTheme.motionScheme` when 1.5.0 ships stable.
 *
 * Also provides theme-resolved [MoneyColors] and keeps the system bar icons legible against the
 * current background.
 */
@Composable
fun MoneyFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }
    // Keyed off the resolved theme rather than the scheme, so these stay correct under Material You.
    val moneyColors = if (darkTheme) DarkMoneyColors else LightMoneyColors

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

    CompositionLocalProvider(LocalMoneyColors provides moneyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoneyFlowTypography,
            shapes = MoneyFlowShapes,
            content = content,
        )
    }
}
