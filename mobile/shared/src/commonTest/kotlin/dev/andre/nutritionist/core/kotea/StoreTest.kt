package dev.andre.nutritionist.core.kotea

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {

    data class TestState(val count: Int = 0)

    sealed interface TestEvent {
        sealed interface Ui : TestEvent {
            data object Increment : Ui
            data object TriggerAsync : Ui
            data object TriggerNews : Ui
        }
        sealed interface Internal : TestEvent {
            data class AsyncCompleted(val value: Int) : Internal
        }
    }

    sealed interface TestCommand {
        data object DoAsync : TestCommand
    }

    sealed interface TestNews {
        data class Message(val text: String) : TestNews
    }

    class TestUpdate : DslUpdate<TestState, TestEvent, TestCommand, TestNews>() {
        override fun NextBuilder<TestState, TestCommand, TestNews>.update(event: TestEvent) {
            when (event) {
                TestEvent.Ui.Increment -> {
                    state { copy(count = count + 1) }
                }
                TestEvent.Ui.TriggerAsync -> {
                    commands(TestCommand.DoAsync)
                }
                TestEvent.Ui.TriggerNews -> {
                    news(TestNews.Message("Hello News"))
                }
                is TestEvent.Internal.AsyncCompleted -> {
                    state { copy(count = count + event.value) }
                }
            }
        }
    }

    class TestCommandsFlowHandler : CommandsFlowHandler<TestCommand, TestEvent> {
        override fun handle(commands: Flow<TestCommand>): Flow<TestEvent> = flow {
            commands.collect { command ->
                when (command) {
                    TestCommand.DoAsync -> emit(TestEvent.Internal.AsyncCompleted(10))
                }
            }
        }
    }

    @Test
    fun `dispatch Ui Increment updates state`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)

        val store = KoteaStoreFactory.create(
            initialState = TestState(0),
            update = TestUpdate(),
            commandsFlowHandler = TestCommandsFlowHandler(),
            scope = scope,
        )

        store.state.test {
            assertEquals(TestState(0), awaitItem())

            store.dispatch(TestEvent.Ui.Increment)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(TestState(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dispatch Ui TriggerAsync executes command and updates state with internal event`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)

        val store = KoteaStoreFactory.create(
            initialState = TestState(0),
            update = TestUpdate(),
            commandsFlowHandler = TestCommandsFlowHandler(),
            scope = scope,
        )

        store.state.test {
            assertEquals(TestState(0), awaitItem())

            store.dispatch(TestEvent.Ui.TriggerAsync)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(TestState(10), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dispatch Ui TriggerNews emits news`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)

        val store = KoteaStoreFactory.create(
            initialState = TestState(0),
            update = TestUpdate(),
            commandsFlowHandler = TestCommandsFlowHandler(),
            scope = scope,
        )

        store.news.test {
            store.dispatch(TestEvent.Ui.TriggerNews)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(TestNews.Message("Hello News"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
