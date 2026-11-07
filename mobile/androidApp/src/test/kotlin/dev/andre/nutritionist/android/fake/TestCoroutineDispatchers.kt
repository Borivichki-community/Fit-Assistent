package dev.andre.nutritionist.android.fake

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Тестовая реализация [CoroutineDispatchers] для unit-тестов в модуле :mobile:androidApp.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineDispatchers(
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : CoroutineDispatchers {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
