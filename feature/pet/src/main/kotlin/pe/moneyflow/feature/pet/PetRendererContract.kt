package pe.moneyflow.feature.pet

/** Runtime-neutral animation intent. The prototype PNG and future Rive rig share this boundary. */
enum class PetAnimationIntent {
    IDLE, BLINK, TAP, HELD, SETTLE, SPEAK, DISMISS, SLEEP, WAKE,
}

/** Complete runtime-neutral input passed to either the Rive renderer or its raster fallback. */
data class PetRendererInput(
    val animationIntent: PetAnimationIntent,
    val lookX: Float = 0f,
    val lookY: Float = 0f,
    val reducedMotion: Boolean,
    val speaking: Boolean,
    val playbackEnabled: Boolean,
) {
    init {
        require(lookX in -1f..1f) { "lookX must be between -1 and 1" }
        require(lookY in -1f..1f) { "lookY must be between -1 and 1" }
    }
}

internal fun PetVisualState.toRendererInput(
    reducedMotion: Boolean,
    playbackEnabled: Boolean,
    lookX: Float = 0f,
    lookY: Float = 0f,
) = PetRendererInput(
    animationIntent = toAnimationIntent(),
    lookX = lookX.coerceIn(-1f, 1f),
    lookY = lookY.coerceIn(-1f, 1f),
    reducedMotion = reducedMotion,
    speaking = this == PetVisualState.SPEAKING,
    playbackEnabled = playbackEnabled,
)

internal fun PetVisualState.toAnimationIntent(): PetAnimationIntent = when (this) {
    PetVisualState.IDLE -> PetAnimationIntent.IDLE
    PetVisualState.BLINKING -> PetAnimationIntent.BLINK
    PetVisualState.TAPPED -> PetAnimationIntent.TAP
    PetVisualState.DRAGGED -> PetAnimationIntent.HELD
    PetVisualState.SETTLING -> PetAnimationIntent.SETTLE
    PetVisualState.SPEAKING -> PetAnimationIntent.SPEAK
    PetVisualState.DISMISSING -> PetAnimationIntent.DISMISS
    PetVisualState.SLEEPING -> PetAnimationIntent.SLEEP
    PetVisualState.WAKING -> PetAnimationIntent.WAKE
}
