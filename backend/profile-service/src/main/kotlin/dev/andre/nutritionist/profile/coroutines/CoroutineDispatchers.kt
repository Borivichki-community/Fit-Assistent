package dev.andre.nutritionist.profile.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Component

/**
 * Интерфейс для инжекции диспетчеров корутин в сервисы и gRPC-слой бэкенда.
 * Полностью соответствует AGENTS.md §1.5 и паттерну из Android/KMP.
 * В тестах заменяется на TestCoroutineDispatchers.
 */
interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

@Component
class DefaultCoroutineDispatchers(
    override val main: CoroutineDispatcher = Dispatchers.Main,
    override val io: CoroutineDispatcher = Dispatchers.IO,
    override val default: CoroutineDispatcher = Dispatchers.Default,
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined,
) : CoroutineDispatchers
