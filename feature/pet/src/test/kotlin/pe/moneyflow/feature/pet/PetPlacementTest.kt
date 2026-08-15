package pe.moneyflow.feature.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PetPlacementTest {
    private val bounds = PetPlacementBounds(
        screenSize = Size(1080f, 2400f), petSize = 240f,
        leftInset = 0f, topInset = 72f, rightInset = 0f, bottomInset = 72f, margin = 24f,
    )

    @Test
    fun `drag bounds protect only system edges`() {
        assertEquals(Offset(24f, 96f), clampToSystemBounds(Offset(-50f, -50f), bounds))
        assertEquals(Offset(816f, 2064f), clampToSystemBounds(Offset(2000f, 3000f), bounds))
    }

    @Test
    fun `release relocates above a critical bottom control`() {
        val nav = Rect(0f, 2050f, 1080f, 2400f)
        val settled = settlePet(Offset(800f, 2000f), bounds, listOf(nav))
        assertFalse(Rect(settled, Size(240f, 240f)).overlaps(nav))
        assertEquals(1786f, settled.y)
    }

    @Test
    fun `release remains exactly in the middle when position is safe`() {
        assertEquals(Offset(420f, 900f), settlePet(Offset(420f, 900f), bounds, emptyList()))
    }

    @Test
    fun `normalized placement restores across a different height`() {
        val original = Offset(816f, 1080f)
        val normalized = normalizedPetY(original, bounds)
        val taller = bounds.copy(screenSize = Size(1080f, 3000f))
        val normalizedX = normalizedPetX(original, bounds)
        val restored = restoredPetPosition(normalizedX, normalized, taller)

        assertEquals(normalizedX, normalizedPetX(restored, taller), 0.001f)
        assertEquals(normalized, normalizedPetY(restored, taller), 0.001f)
    }

    @Test
    fun `speech bubble chooses a side from its measured width`() {
        assertFalse(shouldPlaceBubbleAtEnd(24f, 600f, 1080f, 24f))
        assertEquals(true, shouldPlaceBubbleAtEnd(420f, 770f, 1080f, 24f))
        assertEquals(true, shouldPlaceBubbleAtEnd(816f, 600f, 1080f, 24f))
    }
}
