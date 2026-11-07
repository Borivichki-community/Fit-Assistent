package dev.andre.nutritionist.android.auth

import android.content.Intent
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.AccessToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Реализация [AuthRepository] с использованием AppAuth и [AuthStorage] (EncryptedSharedPreferences).
 */
class AuthRepositoryImpl(
    private val authStorage: AuthStorage,
    private val tokenExchanger: TokenExchanger,
    private val authService: AuthorizationService,
    private val dispatchers: CoroutineDispatchers,
) : AuthRepository {

    private var currentAuthState: AuthState = AuthState()
    private val _authStateFlow = MutableStateFlow(false)

    init {
        // Восстановление сохранённого состояния сессии из безопасного хранилища
        val cachedJson = runBlocking(dispatchers.io) {
            authStorage.getAuthStateJson()
        }
        if (!cachedJson.isNullOrBlank()) {
            runCatching {
                currentAuthState = AuthState.jsonDeserialize(cachedJson)
                _authStateFlow.value = currentAuthState.isAuthorized
            }
        }
    }

    override fun observeAuthState(): Flow<Boolean> = _authStateFlow.asStateFlow()

    override fun getAccessToken(): String? = currentAuthState.accessToken

    override fun getAccessTokenTyped(): AccessToken? =
        currentAuthState.accessToken?.let(::AccessToken)

    override fun isAuthenticated(): Boolean = currentAuthState.isAuthorized

    override suspend fun processAuthResponse(intent: Intent): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val authException = AuthorizationException.fromIntent(intent)
                if (authException != null) {
                    currentAuthState.update(null as AuthorizationResponse?, authException)
                    authStorage.saveAuthStateJson(currentAuthState.jsonSerializeString())
                    _authStateFlow.value = false
                    throw authException
                }

                val authResponse = AuthorizationResponse.fromIntent(intent)
                    ?: throw IllegalArgumentException("Intent does not contain a valid AuthorizationResponse")

                val tokenRequest = authResponse.createTokenExchangeRequest()
                val tokenResponse = tokenExchanger.exchange(tokenRequest)

                currentAuthState.update(tokenResponse, null)
                authStorage.saveAuthStateJson(currentAuthState.jsonSerializeString())
                _authStateFlow.value = currentAuthState.isAuthorized
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            currentAuthState = AuthState()
            authStorage.clear()
            _authStateFlow.value = false
        }
    }

    override suspend fun refreshIfNeeded(): Result<String> = withContext(dispatchers.io) {
        runCatching {
            if (!currentAuthState.isAuthorized) {
                throw IllegalStateException("User is not authorized")
            }
            if (!currentAuthState.needsTokenRefresh) {
                return@runCatching requireNotNull(currentAuthState.accessToken) {
                    "Access token is null despite authState.isAuthorized"
                }
            }

            suspendCancellableCoroutine { continuation ->
                currentAuthState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                    if (ex != null) {
                        continuation.resumeWithException(ex)
                    } else if (accessToken != null) {
                        continuation.resume(accessToken)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Fresh token action returned null token and null exception")
                        )
                    }
                }
            }.also {
                authStorage.saveAuthStateJson(currentAuthState.jsonSerializeString())
            }
        }
    }
}
