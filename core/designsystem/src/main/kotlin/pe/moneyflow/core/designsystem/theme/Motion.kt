package pe.moneyflow.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * The app's motion vocabulary.
 *
 * This exists because Material 3 Expressive's `MaterialTheme.motionScheme` is `internal` in material3
 * 1.4.0 — the Expressive theming surface is only public in the 1.5.0 alpha line, and an alpha is not
 * a dependency this app should carry. These tokens deliver the same two things that matter: motion is
 * **spring-based** (physical and interruptible, rather than a fixed-duration tween that fights a
 * gesture mid-flight), and every animation in the app draws from **one shared set of specs** instead
 * of each call site inventing its own `tween(300)`.
 *
 * The naming mirrors the Expressive scheme so the eventual swap is mechanical:
 * `Motion.spatialDefault` → `MaterialTheme.motionScheme.defaultSpatialSpec()`, and so on.
 *
 * **Spatial** specs move things (position, size, offset) and carry a little bounce, which is what
 * makes motion read as physical. **Effects** specs change non-positional properties (alpha, color)
 * and are critically damped — a bouncing opacity just looks broken.
 */
object Motion {

    /** Position/size changes. Slight overshoot; use for most layout motion. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Snappier spatial motion for small, frequent changes (chevrons, chips, indicators). */
    fun <T> spatialFast(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium,
    )

    /** Slower, more deliberate spatial motion for large surfaces settling into place. */
    fun <T> spatialSlow(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessLow,
    )

    /** Alpha/color. Critically damped — never bounce a non-spatial property. */
    fun <T> effectsDefault(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /**
     * Progress bars and chart growth.
     *
     * Deliberately overshoot-free despite being spatial: a budget bar that bounces past its value and
     * settles back reads, for a moment, as if you had overspent. Motion must not lie about the number.
     */
    fun progress(): FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /** Spatial spec specialised for [Dp] so callers don't need the type argument. */
    fun dp(): FiniteAnimationSpec<Dp> = spatialDefault()

    /** Spatial spec specialised for [IntOffset], for slide transitions. */
    fun offset(): FiniteAnimationSpec<IntOffset> = spatialDefault()
}
