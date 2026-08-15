package pe.moneyflow.feature.pet

/** Stable content keys keep product events separate from user-facing copy. */
enum class PetMessageId {
    INTRODUCTION,
    TAP_REACTION,
    GESTURE_TUTORIAL,
    TRANSACTION_SAVED,
}

internal data class PetMessage(
    val id: PetMessageId,
    val text: String,
    val discreetText: String,
)

private val petMessages = PetMessageId.entries.associateWith { id ->
    when (id) {
        PetMessageId.INTRODUCTION -> PetMessage(
            id, "Hola, soy Castor. Puedes tocarme o moverme.",
            "Hola, soy Castor. Estoy aquí para acompañarte.",
        )
        PetMessageId.TAP_REACTION -> PetMessage(
            id, "¡Estoy aquí para acompañarte!", "¡Estoy aquí contigo!",
        )
        PetMessageId.GESTURE_TUTORIAL -> PetMessage(
            id, "¡Muy bien! Ahora arrástrame y suéltame donde prefieras.",
            "Ahora puedes moverme donde prefieras.",
        )
        PetMessageId.TRANSACTION_SAVED -> PetMessage(
            id, "Movimiento guardado.", "Listo.",
        )
    }
}

internal fun petMessageText(id: PetMessageId, discreetMode: Boolean): String {
    val message = checkNotNull(petMessages[id])
    return if (discreetMode) message.discreetText else message.text
}
