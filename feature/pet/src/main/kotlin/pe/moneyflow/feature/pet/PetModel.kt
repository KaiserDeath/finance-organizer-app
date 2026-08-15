package pe.moneyflow.feature.pet

enum class PetVisualState {
    IDLE, BLINKING, TAPPED, DRAGGED, SPEAKING, DISMISSING, SLEEPING, WAKING, SETTLING,
}

/** Product events are deliberately free of amounts and formatted financial data. */
sealed interface PetEvent {
    data object AppOpened : PetEvent
    data object Tapped : PetEvent
    data object DragStarted : PetEvent
    data object DragEnded : PetEvent
    data object DismissSpeech : PetEvent
    data object InactivityElapsed : PetEvent
    data object Wake : PetEvent
    data object BlinkDue : PetEvent
    data object TransactionSaved : PetEvent
}

data class PetUiState(
    val visualState: PetVisualState = PetVisualState.IDLE,
    val message: String? = null,
    internal val messageId: PetMessageId? = null,
)

internal fun reducePetState(state: PetUiState, event: PetEvent): PetUiState = when (event) {
    PetEvent.AppOpened -> state.withMessage(PetVisualState.SPEAKING, PetMessageId.INTRODUCTION)
    PetEvent.Tapped -> state.withMessage(PetVisualState.TAPPED, PetMessageId.TAP_REACTION)
    PetEvent.DragStarted -> state.withoutMessage(PetVisualState.DRAGGED)
    PetEvent.DragEnded -> state.withoutMessage(PetVisualState.SETTLING)
    PetEvent.DismissSpeech -> state.withoutMessage(PetVisualState.DISMISSING)
    PetEvent.InactivityElapsed -> state.withoutMessage(PetVisualState.SLEEPING)
    PetEvent.Wake -> state.withoutMessage(PetVisualState.WAKING)
    PetEvent.TransactionSaved ->
        state.withMessage(PetVisualState.SPEAKING, PetMessageId.TRANSACTION_SAVED)
    PetEvent.BlinkDue -> if (state.visualState == PetVisualState.IDLE) {
        state.copy(visualState = PetVisualState.BLINKING)
    } else state
}

private fun PetUiState.withMessage(
    visualState: PetVisualState,
    messageId: PetMessageId,
) = copy(
    visualState = visualState,
    message = petMessageText(messageId, discreetMode = false),
    messageId = messageId,
)

private fun PetUiState.withoutMessage(visualState: PetVisualState) = copy(
    visualState = visualState,
    message = null,
    messageId = null,
)
