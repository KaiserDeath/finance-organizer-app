package pe.moneyflow.core.designsystem.component

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import pe.moneyflow.core.designsystem.theme.Motion

/**
 * The app's standard list-item animation. Apply to the direct child of every keyed `items { }` block.
 *
 * Without it, rows **teleport**: delete a transaction and it vanishes while everything below jumps up
 * instantly, then the undo snackbar jumps it back. That happens on all six swipe-to-delete screens,
 * so it's the single most frequently felt piece of missing motion in the app. It also affects filter
 * changes, marking a payment paid, and adding a budget.
 *
 * Uses the shared [Motion] specs so item motion matches the rest of the app rather than Compose's
 * defaults. Requires a `key` on the `items` call — without one Compose cannot tell which row moved.
 */
fun LazyItemScope.animatedItem(): Modifier = Modifier.animateItem(
    fadeInSpec = Motion.effectsDefault(),
    placementSpec = Motion.offset(),
    fadeOutSpec = Motion.effectsDefault(),
)
