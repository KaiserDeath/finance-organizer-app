package pe.moneyflow.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import pe.moneyflow.core.designsystem.theme.Motion

/**
 * Scales a component down slightly while it is held.
 *
 * The cheapest perceived-quality win available: every tap in the app previously got only the stock M3
 * ripple, which confirms *where* you touched but gives the control no sense of physical response.
 * A few percent of scale under the finger makes a button feel like it depresses.
 *
 * Pass the same [InteractionSource] instance you gave the clickable component, so the scale tracks the
 * real press state including drags off the target (which cancel the press and release the scale).
 *
 * Deliberately restrained at 3%: enough to feel, small enough that a full-width button doesn't appear
 * to shrink away from its own layout. Applied to primary CTAs and the FAB — not to list rows, where
 * dozens of independently scaling items would read as instability.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.spatialFast(),
        label = "press-scale",
    )
    return this.scale(scale)
}
