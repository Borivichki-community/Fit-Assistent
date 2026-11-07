package dev.andre.nutritionist.android.auth

import android.content.Intent
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.ResponseTypeValues
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Интерфейс подготовки Intent для запуска системного браузера через Custom Tabs.
 */
interface AuthIntentProvider {
    suspend fun createAuthIntent(): Intent
}

/**
 * Реализация на базе AppAuth с автоматической генерацией PKCE (Code Verifier + S256 Challenge).
 */
class AppAuthIntentProvider(
    private val authConfig: AuthConfig,
    private val authService: AuthorizationService,
    private val dispatchers: CoroutineDispatchers,
) : AuthIntentProvider {

    override suspend fun createAuthIntent(): Intent = withContext(dispatchers.io) {
        val serviceConfig = fetchConfiguration()
        val codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            authConfig.clientId,
            ResponseTypeValues.CODE,
            authConfig.redirectUri,
        )
            .setScope(authConfig.scope)
            .setCodeVerifier(codeVerifier)
            .build()

        authService.getAuthorizationRequestIntent(authRequest)
    }

    private suspend fun fetchConfiguration(): AuthorizationServiceConfiguration =
        suspendCancellableCoroutine { continuation ->
            AuthorizationServiceConfiguration.fetchFromUrl(
                authConfig.discoveryUri,
            ) { config, exception ->
                if (exception != null) {
                    continuation.resumeWithException(exception)
                } else if (config != null) {
                    continuation.resume(config)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Failed to discover OpenID configuration")
                    )
                }
            }
        }
}
