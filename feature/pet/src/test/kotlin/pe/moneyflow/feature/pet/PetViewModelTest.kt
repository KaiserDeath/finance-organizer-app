package pe.moneyflow.feature.pet

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import pe.moneyflow.core.model.PetSpeechFrequency

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `drag end restarts inactivity timer`() = runTest {
        val viewModel = PetViewModel(FakeSettingsRepository())
        runCurrent()

        viewModel.onEvent(PetEvent.DragStarted)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(PetVisualState.DRAGGED, viewModel.state.value.visualState)

        viewModel.onEvent(PetEvent.DragEnded)
        advanceTimeBy(30_001)
        runCurrent()
        assertEquals(PetVisualState.SLEEPING, viewModel.state.value.visualState)
    }

    @Test
    fun `background pauses inactivity and foreground wakes sleeping pet`() = runTest {
        val viewModel = PetViewModel(FakeSettingsRepository())
        runCurrent()

        viewModel.onAppBackgrounded()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(PetVisualState.IDLE, viewModel.state.value.visualState)

        viewModel.onEvent(PetEvent.InactivityElapsed)
        viewModel.onAppForegrounded()
        assertEquals(PetVisualState.WAKING, viewModel.state.value.visualState)
        advanceTimeBy(501)
        runCurrent()
        assertEquals(PetVisualState.IDLE, viewModel.state.value.visualState)

        advanceTimeBy(29_500)
        runCurrent()
        assertEquals(PetVisualState.SLEEPING, viewModel.state.value.visualState)
    }

    @Test
    fun `transaction saved reaction is neutral and respects cooldown`() = runTest {
        val viewModel = PetViewModel(FakeSettingsRepository())
        runCurrent()

        viewModel.onProductEvent(PetProductEvent.TransactionSaved, nowMillis = 1_000L)
        assertEquals(PetVisualState.SPEAKING, viewModel.state.value.visualState)
        assertEquals("Movimiento guardado.", viewModel.state.value.message)

        viewModel.onEvent(PetEvent.DismissSpeech)
        advanceTimeBy(200)
        runCurrent()
        viewModel.onProductEvent(
            PetProductEvent.TransactionSaved,
            nowMillis = 1_000L + TRANSACTION_REACTION_COOLDOWN_MILLIS - 1,
        )
        assertEquals(PetVisualState.IDLE, viewModel.state.value.visualState)

        viewModel.onProductEvent(
            PetProductEvent.TransactionSaved,
            nowMillis = 1_000L + TRANSACTION_REACTION_COOLDOWN_MILLIS,
        )
        assertEquals(PetVisualState.SPEAKING, viewModel.state.value.visualState)
    }

    @Test
    fun `gesture tutorial completes only after tap then drag`() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = PetViewModel(repository)
        runCurrent()

        viewModel.onEvent(PetEvent.DragStarted)
        viewModel.onEvent(PetEvent.DragEnded)
        runCurrent()
        assertEquals(false, repository.current.petGestureOnboardingComplete)

        viewModel.onEvent(PetEvent.Tapped)
        assertEquals(
            "¡Muy bien! Ahora arrástrame y suéltame donde prefieras.",
            viewModel.state.value.message,
        )
        viewModel.onEvent(PetEvent.DragStarted)
        viewModel.onEvent(PetEvent.DragEnded)
        runCurrent()
        assertEquals(true, repository.current.petGestureOnboardingComplete)
    }

    @Test
    fun `discreet mode uses non-financial copy and updates visible speech`() = runTest {
        val repository = FakeSettingsRepository()
        repository.setAmountsHidden(true)
        val viewModel = PetViewModel(repository)
        runCurrent()

        viewModel.onProductEvent(PetProductEvent.TransactionSaved, nowMillis = 1_000L)
        assertEquals("Listo.", viewModel.state.value.message)

        repository.setAmountsHidden(false)
        runCurrent()
        assertEquals("Movimiento guardado.", viewModel.state.value.message)
    }

    @Test
    fun `low frequency uses the longer transaction cooldown`() = runTest {
        val repository = FakeSettingsRepository()
        repository.setPetSpeechFrequency(PetSpeechFrequency.LOW)
        val viewModel = PetViewModel(repository)
        runCurrent()

        viewModel.onProductEvent(PetProductEvent.TransactionSaved, 1_000L)
        viewModel.onEvent(PetEvent.DismissSpeech)
        advanceTimeBy(200)
        runCurrent()
        viewModel.onProductEvent(PetProductEvent.TransactionSaved, 1_000L + 60_000L)
        assertEquals(PetVisualState.IDLE, viewModel.state.value.visualState)

        viewModel.onProductEvent(
            PetProductEvent.TransactionSaved,
            1_000L + LOW_FREQUENCY_REACTION_COOLDOWN_MILLIS,
        )
        assertEquals(PetVisualState.SPEAKING, viewModel.state.value.visualState)
    }

    @Test
    fun `transaction cooldown survives ViewModel recreation`() = runTest {
        val repository = FakeSettingsRepository()
        val first = PetViewModel(repository)
        runCurrent()

        first.onProductEvent(PetProductEvent.TransactionSaved, 1_000L)
        runCurrent()
        assertEquals(1_000L, repository.current.petLastTransactionReactionAt)

        val recreated = PetViewModel(repository)
        runCurrent()
        recreated.onProductEvent(
            PetProductEvent.TransactionSaved,
            1_000L + TRANSACTION_REACTION_COOLDOWN_MILLIS - 1,
        )

        assertEquals(PetVisualState.IDLE, recreated.state.value.visualState)
    }

    @Test
    fun `visible pet state survives process recreation through saved state`() = runTest {
        val repository = FakeSettingsRepository().also {
            it.setPetEnabled(true)
            it.setPetGestureOnboardingComplete(true)
        }
        val savedState = SavedStateHandle()
        val first = PetViewModel(repository, savedState)
        runCurrent()

        first.onEvent(PetEvent.Tapped)
        assertEquals(PetVisualState.TAPPED, first.state.value.visualState)
        assertEquals(PetMessageId.TAP_REACTION, first.state.value.messageId)

        val restored = PetViewModel(repository, savedState)
        runCurrent()

        assertEquals(PetVisualState.TAPPED, restored.state.value.visualState)
        assertEquals(PetMessageId.TAP_REACTION, restored.state.value.messageId)

        advanceTimeBy(2_501)
        runCurrent()
        assertEquals(PetVisualState.IDLE, restored.state.value.visualState)
    }

    @Test
    fun `process recreation converts an interrupted drag into settle`() = runTest {
        val repository = FakeSettingsRepository().also {
            it.setPetEnabled(true)
            it.setPetGestureOnboardingComplete(true)
        }
        val savedState = SavedStateHandle()
        val first = PetViewModel(repository, savedState)
        runCurrent()
        first.onEvent(PetEvent.DragStarted)

        val restored = PetViewModel(repository, savedState)
        runCurrent()
        assertEquals(PetVisualState.SETTLING, restored.state.value.visualState)

        advanceTimeBy(261)
        runCurrent()
        assertEquals(PetVisualState.IDLE, restored.state.value.visualState)
    }

    @Test
    fun `restored message is recomputed for current discreet mode`() = runTest {
        val repository = FakeSettingsRepository().also {
            it.setPetGestureOnboardingComplete(true)
        }
        val savedState = SavedStateHandle()
        val first = PetViewModel(repository, savedState)
        runCurrent()
        first.onProductEvent(PetProductEvent.TransactionSaved, nowMillis = 1_000L)

        repository.setAmountsHidden(true)
        val restored = PetViewModel(repository, savedState)
        runCurrent()

        assertEquals(PetVisualState.SPEAKING, restored.state.value.visualState)
        assertEquals("Listo.", restored.state.value.message)
    }

    @Test
    fun `three quick dismissals temporarily suppress contextual speech`() = runTest {
        val viewModel = PetViewModel(FakeSettingsRepository())
        runCurrent()

        repeat(DISMISSALS_BEFORE_SUPPRESSION) { index ->
            viewModel.onEvent(PetEvent.Tapped)
            viewModel.onSpeechDismissed(nowMillis = index * 1_000L)
            advanceTimeBy(200)
            runCurrent()
        }

        viewModel.onProductEvent(PetProductEvent.TransactionSaved, nowMillis = 3_000L)
        assertEquals(PetVisualState.IDLE, viewModel.state.value.visualState)

        viewModel.onProductEvent(
            PetProductEvent.TransactionSaved,
            nowMillis = 2_000L + REPEATED_DISMISSAL_SUPPRESSION_MILLIS,
        )
        assertEquals(PetVisualState.SPEAKING, viewModel.state.value.visualState)
    }

    @Test
    fun `dismissals outside tracking window do not suppress contextual speech`() = runTest {
        val viewModel = PetViewModel(FakeSettingsRepository())
        runCurrent()

        repeat(DISMISSALS_BEFORE_SUPPRESSION) { index ->
            viewModel.onEvent(PetEvent.Tapped)
            viewModel.onSpeechDismissed(
                nowMillis = index * (DISMISSAL_TRACKING_WINDOW_MILLIS + 1),
            )
            advanceTimeBy(200)
            runCurrent()
        }

        viewModel.onProductEvent(
            PetProductEvent.TransactionSaved,
            nowMillis = DISMISSALS_BEFORE_SUPPRESSION * (DISMISSAL_TRACKING_WINDOW_MILLIS + 1),
        )
        assertEquals(PetVisualState.SPEAKING, viewModel.state.value.visualState)
    }
}
