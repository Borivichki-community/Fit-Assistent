package dev.andre.nutritionist.android.auth

import android.content.Intent
import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.model.AccessToken
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория авторизации для Android-приложения.
 * Расширяет общий [AuthTokenProvider] для сквозной интеграции с gRPC-клиентом.
 */
interface AuthRepository : AuthTokenProvider {

    /**
     * Реактивный поток статуса авторизации (true — пользователь авторизован и есть валидные токены).
     */
    fun observeAuthState(): Flow<Boolean>

    /**
     * Возвращает сырой Access Token (или null, если пользователь не авторизован).
     */
    override fun getAccessToken(): String?

    /**
     * Возвращает строго типизированный [AccessToken] (или null).
     */
    fun getAccessTokenTyped(): AccessToken?

    /**
     * Проверяет, авторизован ли пользователь в данный момент.
     */
    override fun isAuthenticated(): Boolean

    /**
     * Обрабатывает Redirect Intent, полученный от Keycloak:
     * парсит AuthorizationResponse/Exception, обменивает код на токены и сохраняет AuthState.
     */
    suspend fun processAuthResponse(intent: Intent): Result<Unit>

    /**
     * Выход из системы: сброс состояния сессии и очистка токенов из EncryptedSharedPreferences.
     */
    suspend fun logout(): Result<Unit>
}
