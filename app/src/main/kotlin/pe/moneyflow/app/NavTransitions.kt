package pe.moneyflow.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import pe.moneyflow.core.designsystem.theme.Motion
import pe.moneyflow.feature.addedit.MovementDetailRoute

/**
 * Navigation transitions for the app shell.
 *
 * Kept out of `MoneyFlowApp` so the graph itself stays readable, and shared so forward and back are
 * guaranteed to be mirror images — the thing that makes depth legible.
 */

/**
 * True when [entry] renders a `ModalBottomSheet` rather than an ordinary screen.
 *
 * These need **no** navigation transition. The sheet already animates itself up from the bottom
 * scrim; layering a nav transition on top means the content slides twice, at two different speeds,
 * and the scrim fades against a sliding backdrop. Currently only the movement-detail sheet.
 *
 * This is also why there's no shared-element transform from a transaction row into its detail: a
 * `ModalBottomSheet` composes in its own window, outside any `SharedTransitionLayout` scope, so
 * shared bounds cannot be matched across that boundary. Making it work would mean converting the
 * detail from a sheet to an inline destination, which is the wrong trade for a read-first detail.
 */
internal fun isSheetDestination(entry: NavBackStackEntry): Boolean =
    entry.destination.hasRoute(MovementDetailRoute::class)

/** True when both ends of the transition are bottom-nav destinations, i.e. a sibling tab switch. */
internal fun isPeerSwitch(from: NavBackStackEntry, to: NavBackStackEntry): Boolean =
    from.isTopLevelEntry() && to.isTopLevelEntry()

private fun NavBackStackEntry.isTopLevelEntry(): Boolean =
    TopLevelDestination.entries.any { destination.hasRoute(it.route::class) }

// Slide distance as a fraction of container width. A partial slide (rather than a full-width one)
// keeps the motion quick and stops the outgoing screen from feeling flung offscreen.
private const val SlideFraction = 0.18f

/** Forward navigation: the new screen enters from the trailing edge. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInForward(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = Motion.offset(),
        initialOffset = { (it * SlideFraction).toInt() },
    ) + fadeIn(Motion.effectsDefault())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutForward(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = Motion.offset(),
        targetOffset = { (it * SlideFraction).toInt() },
    ) + fadeOut(Motion.effectsDefault())

/** Back navigation: the mirror image, so returning reads as undoing the push. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInBack(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = Motion.offset(),
        initialOffset = { (it * SlideFraction).toInt() },
    ) + fadeIn(Motion.effectsDefault())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutBack(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = Motion.offset(),
        targetOffset = { (it * SlideFraction).toInt() },
    ) + fadeOut(Motion.effectsDefault())

/**
 * Fade-through for sibling tabs: the outgoing screen fades and shrinks a touch, the incoming one
 * fades and grows into place. No direction, because peers have no order.
 */
internal fun fadeThroughIn(): EnterTransition =
    fadeIn(Motion.effectsDefault()) +
        scaleIn(animationSpec = Motion.spatialFast(), initialScale = 0.96f)

internal fun fadeThroughOut(): ExitTransition =
    fadeOut(Motion.effectsDefault()) +
        scaleOut(animationSpec = Motion.spatialFast(), targetScale = 0.96f)
