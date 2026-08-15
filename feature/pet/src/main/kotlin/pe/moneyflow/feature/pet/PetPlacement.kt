package pe.moneyflow.feature.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs

data class PetPlacementBounds(
    val screenSize: Size,
    val petSize: Float,
    val leftInset: Float,
    val topInset: Float,
    val rightInset: Float,
    val bottomInset: Float,
    val margin: Float,
)

/** Hard bounds apply during dragging; they protect only system UI and the physical display. */
internal fun clampToSystemBounds(position: Offset, bounds: PetPlacementBounds): Offset {
    val minX = bounds.leftInset + bounds.margin
    val maxX = (bounds.screenSize.width - bounds.rightInset - bounds.petSize - bounds.margin)
        .coerceAtLeast(minX)
    val minY = bounds.topInset + bounds.margin
    val maxY = (bounds.screenSize.height - bounds.bottomInset - bounds.petSize - bounds.margin)
        .coerceAtLeast(minY)
    return Offset(position.x.coerceIn(minX, maxX), position.y.coerceIn(minY, maxY))
}

/**
 * Chooses the nearest position that does not overlap a critical control. Exclusions affect
 * settling only: while held, the pet can pass over ordinary app content and controls naturally.
 */
internal fun settlePet(
    position: Offset,
    bounds: PetPlacementBounds,
    exclusions: List<Rect>,
): Offset {
    val clamped = clampToSystemBounds(position, bounds)
    val candidates = buildList {
        // The release point wins whenever it is safe. No edge magnetism.
        add(clamped)
        exclusions.forEach { exclusion ->
            add(Offset(clamped.x, exclusion.top - bounds.petSize - bounds.margin))
            add(Offset(clamped.x, exclusion.bottom + bounds.margin))
            add(Offset(exclusion.left - bounds.petSize - bounds.margin, clamped.y))
            add(Offset(exclusion.right + bounds.margin, clamped.y))
        }
    }.map { clampToSystemBounds(it, bounds) }.distinct()

    return candidates
        .filterNot { candidate ->
            val petRect = Rect(candidate, Size(bounds.petSize, bounds.petSize))
            exclusions.any(petRect::overlaps)
        }
        .minByOrNull { candidate -> abs(candidate.x - clamped.x) + abs(candidate.y - clamped.y) }
        ?: clamped
}

internal fun normalizedPetY(position: Offset, bounds: PetPlacementBounds): Float {
    val minY = bounds.topInset + bounds.margin
    val maxY = (bounds.screenSize.height - bounds.bottomInset - bounds.petSize - bounds.margin)
        .coerceAtLeast(minY)
    if (maxY == minY) return 0f
    return ((position.y - minY) / (maxY - minY)).coerceIn(0f, 1f)
}

internal fun normalizedPetX(position: Offset, bounds: PetPlacementBounds): Float {
    val minX = bounds.leftInset + bounds.margin
    val maxX = (bounds.screenSize.width - bounds.rightInset - bounds.petSize - bounds.margin)
        .coerceAtLeast(minX)
    if (maxX == minX) return 0f
    return ((position.x - minX) / (maxX - minX)).coerceIn(0f, 1f)
}

internal fun restoredPetPosition(normalizedX: Float, normalizedY: Float, bounds: PetPlacementBounds): Offset {
    val minX = bounds.leftInset + bounds.margin
    val maxX = (bounds.screenSize.width - bounds.rightInset - bounds.petSize - bounds.margin)
        .coerceAtLeast(minX)
    val minY = bounds.topInset + bounds.margin
    val maxY = (bounds.screenSize.height - bounds.bottomInset - bounds.petSize - bounds.margin)
        .coerceAtLeast(minY)
    return Offset(
        minX + (maxX - minX) * normalizedX.coerceIn(0f, 1f),
        minY + (maxY - minY) * normalizedY.coerceIn(0f, 1f),
    )
}

/** Places a speech bubble toward the side with enough room instead of using a screen-half guess. */
internal fun shouldPlaceBubbleAtEnd(
    petX: Float,
    bubbleWidth: Float,
    hostWidth: Float,
    margin: Float,
): Boolean = bubbleWidth > 0f && petX + bubbleWidth > hostWidth - margin
