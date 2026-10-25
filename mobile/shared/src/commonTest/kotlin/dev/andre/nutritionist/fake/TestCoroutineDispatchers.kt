package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Тестовая реализация CoroutineDispatchers на базе kotlinx-coroutines-test.
 * По умолчанию использует UnconfinedTestDispatcher для синхронного выполнения в тестах.
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
