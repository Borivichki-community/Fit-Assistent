package dev.andre.nutritionist.android.auth

import android.content.Intent
import app.cash.turbine.test
import dev.andre.nutritionist.android.fake.FakeAuthIntentProvider
import dev.andre.nutritionist.android.fake.FakeAuthRepository
import dev.andre.nutritionist.android.fake.TestCoroutineDispatchers
import kotlinx.coroutines.flow.flowOf
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
class AuthCommandsFlowHandlerTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var fakeIntentProvider: FakeAuthIntentProvider
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var handler: AuthCommandsFlowHandler

    @Before
    fun setUp() {
        fakeRepository = FakeAuthRepository()
        fakeIntentProvider = FakeAuthIntentProvider()
        dispatchers = TestCoroutineDispatchers()
        handler = AuthCommandsFlowHandler(
            authRepository = fakeRepository,
            authIntentProvider = fakeIntentProvider,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `LaunchAuthTab command creates Intent and emits AuthIntentPrepared`() = runTest {
        val testIntent = Intent("custom.tabs.intent")
        fakeIntentProvider.intentToReturn = testIntent

        handler.handle(flowOf(AuthCommand.LaunchAuthTab)).test {
            val event = awaitItem()
            assertTrue(event is AuthEvent.Internal.AuthIntentPrepared)
            assertEquals(testIntent, (event as AuthEvent.Internal.AuthIntentPrepared).intent)
            assertEquals(1, fakeIntentProvider.createIntentCallCount)
            awaitComplete()
        }
    }

    @Test
    fun `ProcessAuthResponse command with success result emits AuthSuccess`() = runTest {
        val testIntent = Intent("callback.intent")
        fakeRepository.processResultToReturn = Result.success(Unit)

        handler.handle(flowOf(AuthCommand.ProcessAuthResponse(testIntent))).test {
            val event = awaitItem()
            assertEquals(AuthEvent.Internal.AuthSuccess, event)
            assertEquals(1, fakeRepository.processAuthResponseCallCount)
            awaitComplete()
        }
    }

    @Test
    fun `ProcessAuthResponse command with failure result emits AuthFailed`() = runTest {
        val testIntent = Intent("callback.intent")
        val error = RuntimeException("OAuth code exchange failed")
        fakeRepository.processResultToReturn = Result.failure(error)

        handler.handle(flowOf(AuthCommand.ProcessAuthResponse(testIntent))).test {
            val event = awaitItem()
            assertTrue(event is AuthEvent.Internal.AuthFailed)
            assertEquals(error, (event as AuthEvent.Internal.AuthFailed).error)
            awaitComplete()
        }
    }

    @Test
    fun `Logout command emits LoggedOut event`() = runTest {
        fakeRepository.logoutResultToReturn = Result.success(Unit)

        handler.handle(flowOf(AuthCommand.Logout)).test {
            val event = awaitItem()
            assertEquals(AuthEvent.Internal.LoggedOut, event)
            assertEquals(1, fakeRepository.logoutCallCount)
            awaitComplete()
        }
    }
}
