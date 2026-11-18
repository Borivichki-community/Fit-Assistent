package dev.andre.nutritionist.catalog.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Component

interface CoroutineDispatchers {
    val io: CoroutineDispatcher
}

@Component
class DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
}
