package pe.moneyflow.feature.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetModelTest {
    @Test
    fun `direct manipulation interrupts speech and settles cleanly`() {
        val speaking = reducePetState(PetUiState(), PetEvent.AppOpened)
        val dragged = reducePetState(speaking, PetEvent.DragStarted)
        val settled = reducePetState(dragged, PetEvent.DragEnded)

        assertEquals(PetVisualState.SPEAKING, speaking.visualState)
        assertEquals(PetVisualState.DRAGGED, dragged.visualState)
        assertNull(dragged.message)
        assertEquals(PetVisualState.SETTLING, settled.visualState)
    }

    @Test
    fun `dismiss removes speech and sleep cannot retain a message`() {
        val speaking = reducePetState(PetUiState(), PetEvent.Tapped)
        val dismissed = reducePetState(speaking, PetEvent.DismissSpeech)
        val sleeping = reducePetState(speaking, PetEvent.InactivityElapsed)

        assertEquals(PetVisualState.DISMISSING, dismissed.visualState)
        assertNull(dismissed.message)
        assertEquals(PetVisualState.SLEEPING, sleeping.visualState)
        assertNull(sleeping.message)
    }

    @Test
    fun `blink never interrupts a higher priority state`() {
        val dragged = PetUiState(visualState = PetVisualState.DRAGGED)
        assertEquals(dragged, reducePetState(dragged, PetEvent.BlinkDue))
    }

    @Test
    fun `wake returns a sleeping pet to idle without stale speech`() {
        val sleeping = PetUiState(
            visualState = PetVisualState.SLEEPING,
            message = "stale",
        )

        val awake = reducePetState(sleeping, PetEvent.Wake)

        assertEquals(PetVisualState.WAKING, awake.visualState)
        assertNull(awake.message)
    }

    @Test
    fun `every visual state has a runtime neutral animation intent`() {
        assertEquals(PetVisualState.entries.size, PetVisualState.entries.map { it.toAnimationIntent() }.size)
        assertEquals(PetAnimationIntent.HELD, PetVisualState.DRAGGED.toAnimationIntent())
        assertEquals(PetAnimationIntent.SPEAK, PetVisualState.SPEAKING.toAnimationIntent())
    }

    @Test
    fun `renderer input carries state machine bindings and clamps gaze`() {
        val input = PetVisualState.SPEAKING.toRendererInput(
            reducedMotion = true,
            playbackEnabled = false,
            lookX = 4f,
            lookY = -3f,
        )

        assertEquals(PetAnimationIntent.SPEAK, input.animationIntent)
        assertEquals(1f, input.lookX)
        assertEquals(-1f, input.lookY)
        assertEquals(true, input.reducedMotion)
        assertEquals(true, input.speaking)
        assertEquals(false, input.playbackEnabled)
    }
}
