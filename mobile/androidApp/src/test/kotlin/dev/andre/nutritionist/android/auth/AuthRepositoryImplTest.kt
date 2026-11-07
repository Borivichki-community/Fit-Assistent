package dev.andre.nutritionist.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import dev.andre.nutritionist.android.fake.FakeAuthStorage
import dev.andre.nutritionist.android.fake.FakeTokenExchanger
import dev.andre.nutritionist.android.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryImplTest {

    private lateinit var fakeStorage: FakeAuthStorage
    private lateinit var fakeTokenExchanger: FakeTokenExchanger
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var authService: AuthorizationService
    private lateinit var repository: AuthRepositoryImpl

    private val authConfig = AuthConfig()
    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("http://10.0.2.2:8080/realms/FitAssistant/protocol/openid-connect/auth"),
        Uri.parse("http://10.0.2.2:8080/realms/FitAssistant/protocol/openid-connect/token"),
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeStorage = FakeAuthStorage()
        fakeTokenExchanger = FakeTokenExchanger()
        dispatchers = TestCoroutineDispatchers()
        authService = AuthorizationService(context)

        repository = AuthRepositoryImpl(
            authStorage = fakeStorage,
            tokenExchanger = fakeTokenExchanger,
            authService = authService,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `processAuthResponse with valid Redirect Intent switches state to Authorized and persists tokens`() = runTest {
        // Arrange
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            authConfig.clientId,
            ResponseTypeValues.CODE,
            authConfig.redirectUri,
        ).build()

        val authResponse = AuthorizationResponse.Builder(authRequest)
            .setAuthorizationCode("test-authorization-code-12345")
            .setState(authRequest.state)
            .build()

        val validIntent = Intent().apply {
            putExtra(AuthorizationResponse.EXTRA_RESPONSE, authResponse.jsonSerializeString())
        }

        val expectedAccessToken = "access-token-jwt-secret-payload"
        val expectedRefreshToken = "refresh-token-jwt-secret-payload"
        val tokenResponse = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken(expectedAccessToken)
            .setRefreshToken(expectedRefreshToken)
            .setTokenType("Bearer")
            .setAccessTokenExpirationTime(System.currentTimeMillis() + 3600_000L)
            .build()
        fakeTokenExchanger.tokenResponseToReturn = tokenResponse

        repository.observeAuthState().test {
            // Начальное состояние — не авторизован
            assertEquals(false, awaitItem())

            // Act
            val result = repository.processAuthResponse(validIntent)

            // Assert
            assertTrue(result.isSuccess)
            assertEquals(true, awaitItem())

            assertTrue(repository.isAuthenticated())
            assertEquals(expectedAccessToken, repository.getAccessToken())
            assertEquals(expectedAccessToken, repository.getAccessTokenTyped()?.raw)
            assertEquals(1, fakeTokenExchanger.exchangeCallCount)
            assertEquals(1, fakeStorage.saveCallCount)
            assertNotNull(fakeStorage.savedJson)
        }
    }

    @Test
    fun `processAuthResponse with error intent keeps state Unauthorized and does not grant tokens`() = runTest {
        // Arrange
        val cancelException = AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW
        val errorIntent = cancelException.toIntent()

        repository.observeAuthState().test {
            assertEquals(false, awaitItem())

            // Act
            val result = repository.processAuthResponse(errorIntent)

            // Assert
            assertTrue(result.isFailure)
            assertFalse(repository.isAuthenticated())
            assertNull(repository.getAccessToken())
            assertNull(repository.getAccessTokenTyped())
            assertEquals(0, fakeTokenExchanger.exchangeCallCount)
        }
    }

    @Test
    fun `logout clears stored state and emits unauthorized`() = runTest {
        // Arrange: сначала успешно авторизуемся
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            authConfig.clientId,
            ResponseTypeValues.CODE,
            authConfig.redirectUri,
        ).build()

        val authResponse = AuthorizationResponse.Builder(authRequest)
            .setAuthorizationCode("auth-code")
            .setState(authRequest.state)
            .build()

        val validIntent = Intent().apply {
            putExtra(AuthorizationResponse.EXTRA_RESPONSE, authResponse.jsonSerializeString())
        }

        fakeTokenExchanger.tokenResponseToReturn = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken("token")
            .setTokenType("Bearer")
            .build()

        repository.processAuthResponse(validIntent)
        assertTrue(repository.isAuthenticated())

        repository.observeAuthState().test {
            assertEquals(true, awaitItem())

            // Act: производим logout
            val logoutResult = repository.logout()

            // Assert
            assertTrue(logoutResult.isSuccess)
            assertEquals(false, awaitItem())
            assertFalse(repository.isAuthenticated())
            assertNull(repository.getAccessToken())
            assertEquals(1, fakeStorage.clearCallCount)
            assertNull(fakeStorage.savedJson)
        }
    }
}
