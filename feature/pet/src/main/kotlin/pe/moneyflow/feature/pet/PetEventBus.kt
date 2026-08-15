package pe.moneyflow.feature.pet

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface PetProductEvent {
    /** Contains no amount, title, account, category, or payment information. */
    data object TransactionSaved : PetProductEvent
}

class PetEventBus {
    private val channel = Channel<PetProductEvent>(capacity = Channel.BUFFERED)
    val events: Flow<PetProductEvent> = channel.receiveAsFlow()

    fun publish(event: PetProductEvent): Boolean = channel.trySend(event).isSuccess
}
