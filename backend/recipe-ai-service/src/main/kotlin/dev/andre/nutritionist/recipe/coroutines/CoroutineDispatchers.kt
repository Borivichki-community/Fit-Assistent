package dev.andre.nutritionist.recipe.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Component

/**
 * Интерфейс для инжекции диспетчеров корутин в сервисы и gRPC-слой recipe-ai-service.
 * Полностью соответствует AGENTS.md §1.5.
 */
interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

@Component
class DefaultCoroutineDispatchers(
    override val main: CoroutineDispatcher = Dispatchers.Default,
    override val io: CoroutineDispatcher = Dispatchers.IO,
    override val default: CoroutineDispatcher = Dispatchers.Default,
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined,
) : CoroutineDispatchers
