package pe.moneyflow.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Icon size scale.
 *
 * The app previously used nine ad-hoc sizes (14/16/18/20/22/30/34/40/56dp) with no scale, and
 * several call sites set them with `Modifier.height(n.dp)` instead of `Modifier.size(n.dp)` — which
 * leaves width intrinsic and can distort non-square glyphs. Always pair these with `.size()`.
 */
object IconSize {
    /** Inline with small text — warning glyphs beside labels. */
    val sm = 16.dp

    /** Chip leading/trailing icons. Matches Material's own `*ChipDefaults.IconSize`. */
    val chip = 18.dp

    /** Default for buttons and stat-tile accents. */
    val md = 20.dp

    /** Icon chips (the 40dp rounded containers) and list affordances. */
    val lg = 24.dp

    /** Empty-state and section-header illustration glyphs. */
    val xl = 32.dp

    /** Onboarding pages and the lock screen. */
    val hero = 56.dp
}
