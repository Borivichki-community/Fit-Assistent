package dev.andre.nutritionist.domain.auth

interface AuthTokenProvider {
    fun getAccessToken(): String?
    fun isAuthenticated(): Boolean
    suspend fun refreshIfNeeded(): Result<String>
}