package dev.andre.nutritionist.domain.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Интерфейс для инжекции диспетчеров корутин во все UseCase и Repository.
 * В тестах заменяется на TestCoroutineDispatchers.
 */
interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/**
 * Реализация CoroutineDispatchers по умолчанию для production.
 */
class DefaultCoroutineDispatchers(
    override val main: CoroutineDispatcher = Dispatchers.Main,
    override val io: CoroutineDispatcher = Dispatchers.IO,
    override val default: CoroutineDispatcher = Dispatchers.Default,
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined,
) : CoroutineDispatchers
