package pe.moneyflow.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Discreet mode, as a property of the theme rather than a parameter threaded through screens.
 *
 * A per-screen boolean would have to reach every composable that prints a figure, and the one that
 * gets missed is the one that defeats the feature — a mask with a hole is not a mask. Reading it
 * from the composition means a screen has to opt *out* deliberately, which is the safer default for
 * something whose failure mode is silent.
 *
 * `static` because toggling it should recompose everything that reads it; there is no partial
 * update worth optimising for here, and the toggle is a deliberate, occasional action.
 *
 * Nothing about storage or the domain changes. This is presentation only.
 */
val LocalAmountsHidden = staticCompositionLocalOf { false }
