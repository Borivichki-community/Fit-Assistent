package dev.andre.nutritionist.android.auth

import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Интерфейс обмена Authorization Code на TokenResponse.
 * Позволяет тестировать обмен кодов без выполнения реальных сетевых запросов.
 */
fun interface TokenExchanger {
    suspend fun exchange(request: TokenRequest): TokenResponse
}

/**
 * Стандартная реализация [TokenExchanger] через AppAuth [AuthorizationService].
 */
class AppAuthTokenExchanger(
    private val authService: AuthorizationService,
) : TokenExchanger {

    override suspend fun exchange(request: TokenRequest): TokenResponse =
        suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(request) { response, exception ->
                if (exception != null) {
                    continuation.resumeWithException(exception)
                } else if (response != null) {
                    continuation.resume(response)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Both response and exception are null in AppAuth token callback")
                    )
                }
            }
        }
}
