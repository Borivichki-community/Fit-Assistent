package dev.andre.nutritionist.android.auth

import android.content.Intent
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.andre.nutritionist.android.fake.FakeAuthIntentProvider
import dev.andre.nutritionist.android.fake.FakeAuthRepository
import dev.andre.nutritionist.android.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var fakeIntentProvider: FakeAuthIntentProvider
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeAuthRepository()
        fakeIntentProvider = FakeAuthIntentProvider()
        dispatchers = TestCoroutineDispatchers()

        val update = AuthUpdate()
        val commandsFlowHandler = AuthCommandsFlowHandler(
            authRepository = fakeRepository,
            authIntentProvider = fakeIntentProvider,
            dispatchers = dispatchers,
        )
        val uiStateMapper = AuthUiStateMapper()

        viewModel = AuthViewModel(
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            uiStateMapper = uiStateMapper,
        )
    }

    @Test
    fun `dispatch StartLogin sets Loading state and emits LaunchAuthTab news`() = runTest {
        val testIntent = Intent("custom.auth.intent")
        fakeIntentProvider.intentToReturn = testIntent

        turbineScope {
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val newsTurbine = viewModel.news.testIn(backgroundScope)

            assertEquals(AuthUiState.Content.Idle, stateTurbine.awaitItem().content)

            viewModel.dispatch(AuthEvent.Ui.StartLogin)

            assertEquals(AuthUiState.Content.Loading, stateTurbine.awaitItem().content)

            val news = newsTurbine.awaitItem()
            assertTrue(news is AuthNews.LaunchAuthTab)
            assertEquals(testIntent, (news as AuthNews.LaunchAuthTab).intent)
        }
    }

    @Test
    fun `dispatch AuthResponseReceived on success sets Authorized state and emits NavigateToMain news`() = runTest {
        val testIntent = Intent("callback.intent")
        fakeRepository.processResultToReturn = Result.success(Unit)

        turbineScope {
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val newsTurbine = viewModel.news.testIn(backgroundScope)

            assertEquals(AuthUiState.Content.Idle, stateTurbine.awaitItem().content)

            viewModel.dispatch(AuthEvent.Ui.AuthResponseReceived(testIntent))

            assertEquals(AuthUiState.Content.Loading, stateTurbine.awaitItem().content)
            assertEquals(AuthUiState.Content.Authorized, stateTurbine.awaitItem().content)

            val news = newsTurbine.awaitItem()
            assertEquals(AuthNews.NavigateToMain, news)
        }
    }

    @Test
    fun `dispatch AuthResponseReceived on failure sets Error state and emits ShowSnackbar news`() = runTest {
        val testIntent = Intent("callback.intent")
        val error = RuntimeException("Connection failed")
        fakeRepository.processResultToReturn = Result.failure(error)

        turbineScope {
            val stateTurbine = viewModel.state.testIn(backgroundScope)
            val newsTurbine = viewModel.news.testIn(backgroundScope)

            assertEquals(AuthUiState.Content.Idle, stateTurbine.awaitItem().content)

            viewModel.dispatch(AuthEvent.Ui.AuthResponseReceived(testIntent))

            assertEquals(AuthUiState.Content.Loading, stateTurbine.awaitItem().content)
            assertEquals(AuthUiState.Content.Error("Connection failed"), stateTurbine.awaitItem().content)

            val news = newsTurbine.awaitItem()
            assertEquals(AuthNews.ShowSnackbar("Connection failed"), news)
        }
    }

    @Test
    fun `dispatch LogoutClicked sets Loading then Idle state`() = runTest {
        fakeRepository.logoutResultToReturn = Result.success(Unit)

        turbineScope {
            val stateTurbine = viewModel.state.testIn(backgroundScope)

            assertEquals(AuthUiState.Content.Idle, stateTurbine.awaitItem().content)

            // To properly test logout, we'd transition to Authorized first, or just test LogoutClicked from Idle.
            viewModel.dispatch(AuthEvent.Ui.LogoutClicked)

            assertEquals(AuthUiState.Content.Loading, stateTurbine.awaitItem().content)
            assertEquals(AuthUiState.Content.Idle, stateTurbine.awaitItem().content)
        }
    }
}
