package dev.andre.nutritionist.android.auth

import android.content.Intent
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.AccessToken
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
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
    private val authConfig: AuthConfig? = null,
) : AuthRepository {

    private var currentAuthState: AuthState = AuthState()
    private var currentUserRole: UserRole? = null
    private val _authStateFlow = MutableStateFlow(false)

    init {
        // Восстановление сохранённого состояния сессии из безопасного хранилища
        val cachedJson = authStorage.getAuthStateJsonSync()
        if (!cachedJson.isNullOrBlank()) {
            runCatching {
                currentAuthState = AuthState.jsonDeserialize(cachedJson)
                _authStateFlow.value = currentAuthState.isAuthorized
            }
        }
        val cachedRole = authStorage.getUserRoleSync()
        if (!cachedRole.isNullOrBlank()) {
            currentUserRole = UserRole.entries.firstOrNull { it.name == cachedRole }
        }
    }

    override fun observeAuthState(): Flow<Boolean> = _authStateFlow.asStateFlow()

    override fun getAccessToken(): String? = currentAuthState.accessToken

    override fun getAccessTokenTyped(): AccessToken? =
        currentAuthState.accessToken?.let(::AccessToken)

    override fun getUserId(): UserId? {
        val parsedSub = currentAuthState.parsedIdToken?.subject
        if (!parsedSub.isNullOrBlank()) {
            return UserId(parsedSub)
        }
        val token = currentAuthState.idToken ?: currentAuthState.accessToken ?: return null
        return extractClaimFromJwt(token, "sub")?.let(::UserId)
    }

    override fun getUserDisplayName(): String? {
        val claims = currentAuthState.parsedIdToken?.additionalClaims
        val givenName = claims?.get("given_name") as? String
        if (!givenName.isNullOrBlank()) return givenName

        val name = claims?.get("name") as? String
        if (!name.isNullOrBlank()) return name

        val preferredUsername = claims?.get("preferred_username") as? String
        if (!preferredUsername.isNullOrBlank()) return preferredUsername

        val token = currentAuthState.idToken ?: currentAuthState.accessToken ?: return null
        return extractClaimFromJwt(token, "given_name")
            ?: extractClaimFromJwt(token, "name")
            ?: extractClaimFromJwt(token, "preferred_username")
    }

    override fun getUserEmail(): String? {
        val claims = currentAuthState.parsedIdToken?.additionalClaims
        val email = claims?.get("email") as? String
        if (!email.isNullOrBlank()) return email

        val token = currentAuthState.idToken ?: currentAuthState.accessToken ?: return null
        return extractClaimFromJwt(token, "email")
    }

    override fun getUserRole(): UserRole? {
        currentUserRole?.let { return it }
        if (!currentAuthState.isAuthorized) return null
        val roles = extractRealmRoles()
        return if (roles.contains(ROLE_COACH)) UserRole.COACH else null
    }

    override suspend fun setUserRole(role: UserRole): Unit = withContext(dispatchers.io) {
        currentUserRole = role
        authStorage.saveUserRole(role.name)
    }

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

                currentAuthState.update(authResponse, null)
                currentAuthState.update(tokenResponse, null)
                authStorage.saveAuthStateJson(currentAuthState.jsonSerializeString())
                _authStateFlow.value = currentAuthState.isAuthorized
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            currentAuthState = AuthState()
            currentUserRole = null
            authStorage.clear()
            _authStateFlow.value = false
        }
    }

    override suspend fun refreshIfNeeded(): Result<String> = refreshTokens(force = false)

    override suspend fun forceRefresh(): Result<String> = refreshTokens(force = true)

    private suspend fun refreshTokens(force: Boolean): Result<String> = withContext(dispatchers.io) {
        runCatching {
            if (!currentAuthState.isAuthorized) {
                throw IllegalStateException("User is not authorized")
            }
            if (!force && !currentAuthState.needsTokenRefresh) {
                return@runCatching requireNotNull(currentAuthState.accessToken) {
                    "Access token is null despite authState.isAuthorized"
                }
            }

            try {
                val refreshToken = currentAuthState.refreshToken
                if (force) {
                    if (refreshToken.isNullOrBlank()) {
                        clearSessionLocked()
                        throw IllegalStateException("Refresh token is missing for session refresh")
                    }
                    val tokenRequest = buildRefreshTokenRequest(refreshToken)
                    val tokenResponse = tokenExchanger.exchange(tokenRequest)
                    currentAuthState.update(tokenResponse, null)
                    authStorage.saveAuthStateJson(currentAuthState.jsonSerializeString())
                    _authStateFlow.value = currentAuthState.isAuthorized
                    requireNotNull(currentAuthState.accessToken) {
                        "Access token is null after forced refresh"
                    }
                } else {
                    suspendCancellableCoroutine { continuation ->
                        currentAuthState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                            if (ex != null) {
                                continuation.resumeWithException(ex)
                            } else if (accessToken != null) {
                                continuation.resume(accessToken)
                            } else {
                                continuation.resumeWithException(
                                    IllegalStateException("Fresh token action returned null token and null exception"),
                                )
                            }
                        }
                    }.also {
                        authStorage.saveAuthStateJson(currentAuthState.jsonSerializeString())
                    }
                }
            } catch (ex: AuthorizationException) {
                if (ex.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
                    clearSessionLocked()
                }
                throw ex
            } catch (ex: IllegalStateException) {
                if (force) {
                    clearSessionLocked()
                }
                throw ex
            }
        }
    }

    private fun buildRefreshTokenRequest(refreshToken: String): TokenRequest {
        val existingConfig = currentAuthState.authorizationServiceConfiguration
        if (existingConfig != null) {
            return currentAuthState.createTokenRefreshRequest()
        }
        val fallbackAuth = authConfig?.authEndpoint
        val fallbackToken = authConfig?.tokenEndpoint
        if (fallbackAuth != null && fallbackToken != null) {
            val config = AuthorizationServiceConfiguration(fallbackAuth, fallbackToken)
            val clientId = authConfig.clientId
            return TokenRequest.Builder(config, clientId)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(refreshToken)
                .build()
        }
        throw IllegalStateException("No authorization configuration available for refresh request")
    }

    private suspend fun clearSessionLocked() {
        currentAuthState = AuthState()
        currentUserRole = null
        authStorage.clear()
        _authStateFlow.value = false
    }

    private fun extractRealmRoles(): Set<String> {
        val token = currentAuthState.accessToken ?: currentAuthState.idToken ?: return emptySet()
        return runCatching {
            val parts = token.split(".")
            if (parts.size < 2) return emptySet()
            val payloadBytes = android.util.Base64.decode(
                parts[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
            )
            val json = org.json.JSONObject(String(payloadBytes, Charsets.UTF_8))
            val rolesArray = json.optJSONObject("realm_access")?.optJSONArray("roles") ?: return emptySet()
            buildSet {
                for (i in 0 until rolesArray.length()) {
                    val role = rolesArray.optString(i)
                    if (role.isNotBlank()) add(role)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun extractClaimFromJwt(token: String, claimName: String): String? = runCatching {
        val parts = token.split(".")
        if (parts.size >= 2) {
            val payloadBytes = android.util.Base64.decode(
                parts[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
            )
            val jsonString = String(payloadBytes, Charsets.UTF_8)
            val regex = """"$claimName"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(jsonString)?.groupValues?.get(1)
        } else {
            null
        }
    }.getOrNull()

    private companion object {
        const val ROLE_COACH = "ROLE_COACH"
    }
}
