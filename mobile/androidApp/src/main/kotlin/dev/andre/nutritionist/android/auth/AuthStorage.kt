package dev.andre.nutritionist.android.auth

/**
 * Интерфейс абстрактного безопасного хранилища состояния авторизации (JSON-строка AuthState).
 * Позволяет изолировать доменную логику и тесты от системного Android Keystore с помощью Fakes.
 */
interface AuthStorage {
    suspend fun getAuthStateJson(): String?
    suspend fun saveAuthStateJson(json: String)
    suspend fun clear()
}
