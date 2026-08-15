package pe.moneyflow.core.ui.safearea

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

class SafeAreaRegistry {
    private val areas = mutableStateMapOf<String, Rect>()

    val exclusions: List<Rect> get() = areas.values.toList()

    fun update(key: String, bounds: Rect) {
        areas[key] = bounds
    }

    fun remove(key: String) {
        areas.remove(key)
    }

    fun clear() {
        areas.clear()
    }
}

val LocalSafeAreaRegistry = staticCompositionLocalOf<SafeAreaRegistry?> { null }

/** Publishes measured window bounds and removes stale bounds when the surface leaves composition. */
@Composable
fun Modifier.safeArea(key: String): Modifier {
    val registry = LocalSafeAreaRegistry.current ?: return this
    DisposableEffect(registry, key) {
        onDispose { registry.remove(key) }
    }
    return onGloballyPositioned { registry.update(key, it.boundsInWindow()) }
}

fun Modifier.safeArea(key: String, registry: SafeAreaRegistry): Modifier =
    onGloballyPositioned { registry.update(key, it.boundsInWindow()) }
