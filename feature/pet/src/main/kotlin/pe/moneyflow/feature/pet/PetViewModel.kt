package pe.moneyflow.feature.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.PetSpeechFrequency

data class PetPreferencesUiState(
    val enabled: Boolean = false,
    val reducedMotion: Boolean = false,
    val speechEnabled: Boolean = true,
    val speechFrequency: PetSpeechFrequency = PetSpeechFrequency.NORMAL,
    val discreetMode: Boolean = false,
    val gestureOnboardingComplete: Boolean = false,
    val normalizedX: Float = 1f,
    val normalizedY: Float = 1f,
)

@HiltViewModel
class PetViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoredPetState(savedStateHandle))
    val state: StateFlow<PetUiState> = mutableState
    val preferences = settingsRepository.preferences
        .map {
            PetPreferencesUiState(
                enabled = it.petEnabled,
                reducedMotion = it.petReducedMotion,
                speechEnabled = it.petSpeechEnabled,
                speechFrequency = it.petSpeechFrequency,
                discreetMode = it.amountsHidden,
                gestureOnboardingComplete = it.petGestureOnboardingComplete,
                normalizedX = it.petPositionX,
                normalizedY = it.petPositionY,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetPreferencesUiState())

    private var returnJob: Job? = null
    private var sleepJob: Job? = null
    private var lastTransactionReactionAt: Long? = null
    private val recentSpeechDismissals = ArrayDeque<Long>()
    private var contextualSpeechSuppressedUntil: Long? = null

    private var speechEnabled = true
    private var speechFrequency = PetSpeechFrequency.NORMAL
    private var discreetMode = false
    private var gestureOnboardingComplete = false
    private var onboardingTapped = false

    init {
        viewModelScope.launch {
            val initial = settingsRepository.preferences.first()
            speechFrequency = initial.petSpeechFrequency
            speechEnabled = speechFrequency != PetSpeechFrequency.SILENT
            discreetMode = initial.amountsHidden
            mutableState.value.messageId?.let { messageId ->
                updateState(mutableState.value.copy(message = petMessageText(messageId, discreetMode)))
            }
            lastTransactionReactionAt = initial.petLastTransactionReactionAt
            gestureOnboardingComplete = initial.petGestureOnboardingComplete
            if (initial.petEnabled) {
                if (!initial.petIntroductionComplete) {
                    onEvent(PetEvent.AppOpened)
                    settingsRepository.setPetIntroductionComplete(true)
                }
                resumeRestoredTransition()
                scheduleSleep()
            }
        }
        viewModelScope.launch {
            settingsRepository.preferences.collect {
                speechFrequency = it.petSpeechFrequency
                speechEnabled = speechFrequency != PetSpeechFrequency.SILENT
                gestureOnboardingComplete = it.petGestureOnboardingComplete
                if (discreetMode != it.amountsHidden) {
                    discreetMode = it.amountsHidden
                    mutableState.value.messageId?.let { messageId ->
                        updateState(mutableState.value.copy(
                            message = petMessageText(messageId, discreetMode),
                        ))
                    }
                }
            }
        }
    }

    fun onEvent(event: PetEvent) {
        if (!speechEnabled && (event == PetEvent.AppOpened || event == PetEvent.TransactionSaved)) return
        returnJob?.cancel()
        val reduced = reducePetState(mutableState.value, event)
        updateState(when {
            !speechEnabled -> reduced.copy(message = null, messageId = null)
            event == PetEvent.Tapped && !gestureOnboardingComplete -> reduced.copy(
                message = petMessageText(PetMessageId.GESTURE_TUTORIAL, discreetMode),
                messageId = PetMessageId.GESTURE_TUTORIAL,
            )
            reduced.messageId != null -> reduced.copy(
                message = petMessageText(reduced.messageId, discreetMode),
            )
            else -> reduced
        })
        when (event) {
            PetEvent.DragStarted -> sleepJob?.cancel()
            PetEvent.DragEnded -> {
                returnToIdle(260)
                scheduleSleep()
                if (!gestureOnboardingComplete && onboardingTapped) {
                    gestureOnboardingComplete = true
                    viewModelScope.launch { settingsRepository.setPetGestureOnboardingComplete(true) }
                }
            }
            PetEvent.DismissSpeech -> {
                returnToIdle(160)
                scheduleSleep()
            }
            PetEvent.BlinkDue -> returnToIdle(120)
            PetEvent.Tapped -> {
                onboardingTapped = true
                returnToIdle(2_500)
                scheduleSleep()
            }
            PetEvent.Wake -> {
                returnToIdle(500)
                scheduleSleep()
            }
            else -> Unit
        }
    }

    fun onProductEvent(event: PetProductEvent, nowMillis: Long = System.currentTimeMillis()) {
        if (contextualSpeechSuppressedUntil?.let { nowMillis < it } == true) return
        when (event) {
            PetProductEvent.TransactionSaved -> {
                val last = lastTransactionReactionAt
                val cooldown = when (speechFrequency) {
                    PetSpeechFrequency.NORMAL -> TRANSACTION_REACTION_COOLDOWN_MILLIS
                    PetSpeechFrequency.LOW -> LOW_FREQUENCY_REACTION_COOLDOWN_MILLIS
                    PetSpeechFrequency.SILENT -> return
                }
                if (last != null && nowMillis - last < cooldown) return
                lastTransactionReactionAt = nowMillis
                viewModelScope.launch {
                    settingsRepository.setPetLastTransactionReactionAt(nowMillis)
                }
                onEvent(PetEvent.TransactionSaved)
                returnToIdle(4_000)
                scheduleSleep()
            }
        }
    }

    /** Manual bubble closes are a signal to make automatic reactions less insistent. */
    fun onSpeechDismissed(nowMillis: Long = System.currentTimeMillis()) {
        while (
            recentSpeechDismissals.isNotEmpty() &&
            nowMillis - recentSpeechDismissals.first() > DISMISSAL_TRACKING_WINDOW_MILLIS
        ) {
            recentSpeechDismissals.removeFirst()
        }
        recentSpeechDismissals.addLast(nowMillis)
        if (recentSpeechDismissals.size >= DISMISSALS_BEFORE_SUPPRESSION) {
            contextualSpeechSuppressedUntil = nowMillis + REPEATED_DISMISSAL_SUPPRESSION_MILLIS
            recentSpeechDismissals.clear()
        }
        onEvent(PetEvent.DismissSpeech)
    }

    fun onAppForegrounded() {
        if (mutableState.value.visualState == PetVisualState.SLEEPING) {
            onEvent(PetEvent.Wake)
        } else {
            scheduleSleep()
        }
    }

    fun onAppBackgrounded() {
        returnJob?.cancel()
        sleepJob?.cancel()
    }

    private fun returnToIdle(afterMillis: Long) {
        returnJob = viewModelScope.launch {
            delay(afterMillis)
            updateState(mutableState.value.copy(
                visualState = PetVisualState.IDLE,
                message = null,
                messageId = null,
            ))
        }
    }

    private fun scheduleSleep() {
        sleepJob?.cancel()
        sleepJob = viewModelScope.launch {
            delay(30_000)
            onEvent(PetEvent.InactivityElapsed)
        }
    }

    private fun resumeRestoredTransition() {
        when (mutableState.value.visualState) {
            PetVisualState.BLINKING -> returnToIdle(120)
            PetVisualState.DISMISSING -> returnToIdle(160)
            PetVisualState.SETTLING -> returnToIdle(260)
            PetVisualState.WAKING -> returnToIdle(500)
            PetVisualState.TAPPED -> returnToIdle(2_500)
            PetVisualState.SPEAKING -> returnToIdle(4_000)
            PetVisualState.DRAGGED -> {
                updateState(PetUiState(visualState = PetVisualState.SETTLING))
                returnToIdle(260)
            }
            PetVisualState.IDLE,
            PetVisualState.SLEEPING,
            -> Unit
        }
    }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setPetEnabled(enabled)
    }

    fun setReducedMotion(reduced: Boolean) = viewModelScope.launch {
        settingsRepository.setPetReducedMotion(reduced)
    }

    fun setSpeechEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setPetSpeechFrequency(
            if (enabled) PetSpeechFrequency.NORMAL else PetSpeechFrequency.SILENT,
        )
        if (!enabled && mutableState.value.message != null) onEvent(PetEvent.DismissSpeech)
    }

    fun setSpeechFrequency(frequency: PetSpeechFrequency) = viewModelScope.launch {
        settingsRepository.setPetSpeechFrequency(frequency)
        if (frequency == PetSpeechFrequency.SILENT && mutableState.value.message != null) {
            onEvent(PetEvent.DismissSpeech)
        }
    }

    fun resetPosition() = viewModelScope.launch {
        settingsRepository.setPetPlacement(1f, 1f)
    }

    fun replayIntroduction() = viewModelScope.launch {
        settingsRepository.setPetIntroductionComplete(true)
        onEvent(PetEvent.AppOpened)
    }

    fun replayGestureOnboarding() = viewModelScope.launch {
        onboardingTapped = false
        gestureOnboardingComplete = false
        settingsRepository.setPetGestureOnboardingComplete(false)
        onEvent(PetEvent.AppOpened)
    }

    fun savePlacement(normalizedX: Float, normalizedY: Float) = viewModelScope.launch {
        settingsRepository.setPetPlacement(normalizedX, normalizedY)
    }

    private fun updateState(state: PetUiState) {
        mutableState.value = state
        savedStateHandle[PET_VISUAL_STATE_KEY] = state.visualState.name
        savedStateHandle[PET_MESSAGE_ID_KEY] = state.messageId?.name
    }
}

private fun restoredPetState(savedStateHandle: SavedStateHandle): PetUiState {
    val visualState = savedStateHandle.get<String>(PET_VISUAL_STATE_KEY)
        ?.let { runCatching { PetVisualState.valueOf(it) }.getOrNull() }
        ?: PetVisualState.IDLE
    val messageId = savedStateHandle.get<String>(PET_MESSAGE_ID_KEY)
        ?.let { runCatching { PetMessageId.valueOf(it) }.getOrNull() }
    return PetUiState(
        visualState = visualState,
        message = messageId?.let { petMessageText(it, discreetMode = false) },
        messageId = messageId,
    )
}

private const val PET_VISUAL_STATE_KEY = "petVisualState"
private const val PET_MESSAGE_ID_KEY = "petMessageId"

internal const val TRANSACTION_REACTION_COOLDOWN_MILLIS = 60_000L
internal const val LOW_FREQUENCY_REACTION_COOLDOWN_MILLIS = 5 * 60_000L
internal const val DISMISSAL_TRACKING_WINDOW_MILLIS = 10 * 60_000L
internal const val REPEATED_DISMISSAL_SUPPRESSION_MILLIS = 30 * 60_000L
internal const val DISMISSALS_BEFORE_SUPPRESSION = 3
