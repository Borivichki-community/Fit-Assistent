package dev.andre.nutritionist.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import dev.andre.nutritionist.android.fake.FakeAuthStorage
import dev.andre.nutritionist.android.fake.FakeTokenExchanger
import dev.andre.nutritionist.android.fake.TestCoroutineDispatchers
import dev.andre.nutritionist.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthState
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
            authConfig = authConfig,
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

    @Test
    fun `refreshIfNeeded returns failure when user is not authorized`() = runTest {
        // Act: вызов без предварительной авторизации
        val result = repository.refreshIfNeeded()

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("User is not authorized", result.exceptionOrNull()?.message)
    }

    @Test
    fun `refreshIfNeeded returns current access token when token is fresh`() = runTest {
        // Arrange: авторизуемся с токеном со сроком жизни +1 час
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

        val expectedToken = "fresh-access-token-123"
        fakeTokenExchanger.tokenResponseToReturn = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken(expectedToken)
            .setTokenType("Bearer")
            .setAccessTokenExpirationTime(System.currentTimeMillis() + 3600_000L)
            .build()

        repository.processAuthResponse(validIntent)
        assertTrue(repository.isAuthenticated())

        // Act: запрашиваем рефреш
        val result = repository.refreshIfNeeded()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedToken, result.getOrNull())
    }

    @Test
    fun `getUserDisplayName returns null when not authorized`() {
        assertNull(repository.getUserDisplayName())
    }

    @Test
    fun `getUserDisplayName extracts given_name from id token jwt payload`() = runTest {
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

        val mockIdToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTQyIiwiZ2l2ZW5fbmFtZSI6IkFsZWtzZXkiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhbGV4In0."

        fakeTokenExchanger.tokenResponseToReturn = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken("test-access-token")
            .setIdToken(mockIdToken)
            .setTokenType("Bearer")
            .build()

        repository.processAuthResponse(validIntent)
        assertTrue(repository.isAuthenticated())

        assertEquals("Aleksey", repository.getUserDisplayName())
    }

    @Test
    fun `getUserDisplayName falls back to preferred_username when given_name is missing`() = runTest {
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

        val mockIdToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTQyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxleDk5In0."

        fakeTokenExchanger.tokenResponseToReturn = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken("test-access-token")
            .setIdToken(mockIdToken)
            .setTokenType("Bearer")
            .build()

        repository.processAuthResponse(validIntent)
        assertTrue(repository.isAuthenticated())

        assertEquals("alex99", repository.getUserDisplayName())
    }

    @Test
    fun `initialization restores cached session synchronously from authStorage without runBlocking`() = runTest {
        val authState = AuthState(serviceConfig)
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            authConfig.clientId,
            ResponseTypeValues.CODE,
            authConfig.redirectUri,
        ).build()
        val authResponse = AuthorizationResponse.Builder(authRequest)
            .setAuthorizationCode("auth-code")
            .build()
        val tokenResponse = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken("cached-token-12345")
            .setTokenType("Bearer")
            .build()
        authState.update(authResponse, null)
        authState.update(tokenResponse, null)

        val storageWithSession = FakeAuthStorage(initialJson = authState.jsonSerializeString())
        val repo = AuthRepositoryImpl(
            authStorage = storageWithSession,
            tokenExchanger = fakeTokenExchanger,
            authService = authService,
            dispatchers = dispatchers,
        )

        assertTrue(repo.isAuthenticated())
        assertEquals("cached-token-12345", repo.getAccessToken())
        repo.observeAuthState().test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setUserRole persists role and logout clears it`() = runTest {
        assertNull(repository.getUserRole())

        repository.setUserRole(UserRole.COACH)

        assertEquals(UserRole.COACH, repository.getUserRole())
        assertEquals("COACH", fakeStorage.savedRole)

        repository.logout()

        assertNull(repository.getUserRole())
        assertNull(fakeStorage.savedRole)
    }

    @Test
    fun `initialization restores persisted user role from authStorage`() {
        val storageWithRole = FakeAuthStorage(initialRole = UserRole.CLIENT.name)
        val repo = AuthRepositoryImpl(
            authStorage = storageWithRole,
            tokenExchanger = fakeTokenExchanger,
            authService = authService,
            dispatchers = dispatchers,
        )

        assertEquals(UserRole.CLIENT, repo.getUserRole())
    }

    @Test
    fun `getUserEmail extracts email claim from id token jwt payload and returns null when unauthorized`() = runTest {
        assertNull(repository.getUserEmail())

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

        val mockIdToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTQyIiwiZW1haWwiOiJhbGV4QGV4YW1wbGUuY29tIn0."

        fakeTokenExchanger.tokenResponseToReturn = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken("test-access-token")
            .setIdToken(mockIdToken)
            .setTokenType("Bearer")
            .build()

        repository.processAuthResponse(validIntent)

        assertEquals("alex@example.com", repository.getUserEmail())
    }

    @Test
    fun `getUserRole extracts ROLE_COACH from access token realm_access claims when not explicitly persisted`() = runTest {
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

        val payloadJson = """{"sub":"coach-42","realm_access":{"roles":["ROLE_USER","ROLE_COACH"]}}"""
        val encodedPayload = android.util.Base64.encodeToString(
            payloadJson.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
        )
        val mockAccessToken = "eyJhbGciOiJub25lIn0.$encodedPayload."

        fakeTokenExchanger.tokenResponseToReturn = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken(mockAccessToken)
            .setTokenType("Bearer")
            .build()

        repository.processAuthResponse(validIntent)

        assertEquals(UserRole.COACH, repository.getUserRole())
    }

    @Test
    fun `forceRefresh exchanges refresh token even when local access token is still fresh`() = runTest {
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
            .setAccessToken("initial-jwt")
            .setRefreshToken("refresh-token-1")
            .setTokenType("Bearer")
            .setAccessTokenExpirationTime(System.currentTimeMillis() + 3600_000L)
            .build()
        repository.processAuthResponse(validIntent)

        val refreshReq = TokenResponse.Builder(authResponse.createTokenExchangeRequest())
            .setAccessToken("refreshed-jwt")
            .setRefreshToken("refresh-token-2")
            .setTokenType("Bearer")
            .build()
        fakeTokenExchanger.tokenResponseToReturn = refreshReq

        val result = repository.forceRefresh()

        assertTrue(result.isSuccess)
        assertEquals("refreshed-jwt", result.getOrNull())
        assertEquals("refreshed-jwt", repository.getAccessToken())
    }

    @Test
    fun `forceRefresh clears session when refresh token is missing`() = runTest {
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
            .setAccessToken("zombie-jwt-without-refresh")
            .setTokenType("Bearer")
            .build()
        repository.processAuthResponse(validIntent)
        assertTrue(repository.isAuthenticated())

        val result = repository.forceRefresh()

        assertTrue(result.isFailure)
        assertEquals(false, repository.isAuthenticated())
        assertNull(repository.getAccessToken())
    }
}
