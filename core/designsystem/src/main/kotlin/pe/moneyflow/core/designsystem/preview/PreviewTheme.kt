package pe.moneyflow.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.designsystem.theme.Spacing

/**
 * Wrapper for `@Preview` composables.
 *
 * Pins `dynamicColor = false` so previews always show the brand palette rather than the IDE's
 * wallpaper stand-in, and paints the real `background` behind the content so surface/container
 * colors can be judged in context. [darkTheme] follows the preview's `uiMode`, which is what makes
 * [ThemePreviews] work.
 */
@Composable
fun MoneyFlowPreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    padded: Boolean = true,
    content: @Composable () -> Unit,
) {
    MoneyFlowTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = if (padded) Modifier.padding(Spacing.lg) else Modifier) {
                content()
            }
        }
    }
}

/** Light and dark in one shot — the minimum bar for reviewing any component. */
@Preview(name = "Light", group = "theme")
@Preview(name = "Dark", group = "theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/**
 * Default and maximum-ish font scale.
 *
 * Worth applying to anything that renders a currency amount: the 200% variant is what surfaces
 * clipped or truncated figures, which is how `StatTile` was silently ellipsizing amounts to
 * "S/ 1,2…" for anyone using large text.
 */
@Preview(name = "Font 100%", group = "scale", fontScale = 1.0f)
@Preview(name = "Font 200%", group = "scale", fontScale = 2.0f)
annotation class FontScalePreviews

/** Light/dark crossed with default/large text — four renders, for amount-bearing components. */
@Preview(name = "Light · 100%", group = "full", fontScale = 1.0f)
@Preview(name = "Light · 200%", group = "full", fontScale = 2.0f)
@Preview(
    name = "Dark · 100%",
    group = "full",
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Dark · 200%",
    group = "full",
    fontScale = 2.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ThemeAndScalePreviews
