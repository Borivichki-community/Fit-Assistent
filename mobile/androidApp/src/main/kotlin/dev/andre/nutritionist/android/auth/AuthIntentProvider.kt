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
import net.openid.appauth.connectivity.ConnectionBuilder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Интерфейс подготовки Intent для запуска системного браузера через Custom Tabs.
 */
interface AuthIntentProvider {
    suspend fun createAuthIntent(
        idpHint: String? = null,
        prompt: String? = null,
    ): Intent
}

/**
 * Реализация на базе AppAuth с автоматической генерацией PKCE (Code Verifier + S256 Challenge).
 */
class AppAuthIntentProvider(
    private val authConfig: AuthConfig,
    private val authService: AuthorizationService,
    private val dispatchers: CoroutineDispatchers,
    private val connectionBuilder: ConnectionBuilder = AppAuthConnectionBuilder,
) : AuthIntentProvider {

    override suspend fun createAuthIntent(
        idpHint: String?,
        prompt: String?,
    ): Intent = withContext(dispatchers.io) {
        val serviceConfig = fetchConfiguration()
        val codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()

        val requestBuilder = AuthorizationRequest.Builder(
            serviceConfig,
            authConfig.clientId,
            ResponseTypeValues.CODE,
            authConfig.redirectUri,
        )
            .setScope(authConfig.scope)
            .setCodeVerifier(codeVerifier)

        if (!prompt.isNullOrBlank()) {
            requestBuilder.setPrompt(prompt)
        }

        val additionalParams = mutableMapOf<String, String>()
        if (!idpHint.isNullOrBlank()) {
            additionalParams["kc_idp_hint"] = idpHint
        }
        if (additionalParams.isNotEmpty()) {
            requestBuilder.setAdditionalParameters(additionalParams)
        }

        val authRequest = requestBuilder.build()
        authService.getAuthorizationRequestIntent(authRequest)
    }

    private suspend fun fetchConfiguration(): AuthorizationServiceConfiguration =
        withContext(dispatchers.io) {
            val authEndpoint = authConfig.authEndpoint
            val tokenEndpoint = authConfig.tokenEndpoint
            if ((authEndpoint != null) && (tokenEndpoint != null)) {
                AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)
            } else {
                fetchFromDiscovery()
            }
        }

    private suspend fun fetchFromDiscovery(): AuthorizationServiceConfiguration =
        suspendCancellableCoroutine { continuation ->
            AuthorizationServiceConfiguration.fetchFromUrl(
                authConfig.discoveryUri,
                { config, exception ->
                    if (exception != null) {
                        continuation.resumeWithException(exception)
                    } else if (config != null) {
                        continuation.resume(config)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Failed to discover OpenID configuration"),
                        )
                    }
                },
                connectionBuilder,
            )
        }
}
