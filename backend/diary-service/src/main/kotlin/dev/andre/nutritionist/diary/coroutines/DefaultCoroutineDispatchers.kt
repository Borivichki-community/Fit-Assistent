package dev.andre.nutritionist.diary.coroutines

import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Component

@Component
class DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val main = Dispatchers.Main
    override val io = Dispatchers.IO
    override val default = Dispatchers.Default
    override val unconfined = Dispatchers.Unconfined
}
