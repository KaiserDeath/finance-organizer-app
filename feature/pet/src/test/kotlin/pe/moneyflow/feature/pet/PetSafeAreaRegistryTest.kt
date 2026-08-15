package pe.moneyflow.feature.pet

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.moneyflow.core.ui.safearea.SafeAreaRegistry

class PetSafeAreaRegistryTest {
    @Test
    fun `registry updates and removes measured controls by stable key`() {
        val registry = SafeAreaRegistry()
        val original = Rect(0f, 2_000f, 200f, 2_300f)
        val resized = Rect(0f, 200f, 120f, 900f)

        registry.update("navigation_dashboard", original)
        registry.update("navigation_dashboard", resized)
        assertEquals(listOf(resized), registry.exclusions)

        registry.remove("navigation_dashboard")
        assertTrue(registry.exclusions.isEmpty())
    }
}
