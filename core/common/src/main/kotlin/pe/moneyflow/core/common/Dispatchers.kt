package pe.moneyflow.core.common

import javax.inject.Qualifier

/** Marks the IO-bound [kotlinx.coroutines.CoroutineDispatcher] for injection. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the CPU-bound (Default) [kotlinx.coroutines.CoroutineDispatcher] for injection. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
