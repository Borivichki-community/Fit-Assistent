package dev.andre.nutritionist.android.fake

import android.content.Intent
import dev.andre.nutritionist.android.auth.AuthIntentProvider
import dev.andre.nutritionist.android.auth.AuthRepository
import dev.andre.nutritionist.domain.model.AccessToken
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ручной Fake для [AuthRepository].
 */
class FakeAuthRepository : AuthRepository {

    private val _state = MutableStateFlow(false)
    var currentToken: String? = null
    var currentUserId: UserId? = UserId("fake-user-001")
    var currentUserRole: UserRole? = null
    var processResultToReturn: Result<Unit> = Result.success(Unit)
    var logoutResultToReturn: Result<Unit> = Result.success(Unit)

    var processAuthResponseCallCount: Int = 0
    var logoutCallCount: Int = 0
    var setUserRoleCallCount: Int = 0

    override fun observeAuthState(): Flow<Boolean> = _state.asStateFlow()

    override fun getAccessToken(): String? = currentToken

    override fun getAccessTokenTyped(): AccessToken? = currentToken?.let(::AccessToken)

    override fun getUserId(): UserId? = currentUserId

    var userDisplayNameToReturn: String? = null

    override fun getUserDisplayName(): String? = userDisplayNameToReturn

    override fun getUserRole(): UserRole? = currentUserRole

    override suspend fun setUserRole(role: UserRole) {
        setUserRoleCallCount++
        currentUserRole = role
    }

    override fun isAuthenticated(): Boolean = _state.value

    override suspend fun processAuthResponse(intent: Intent): Result<Unit> {
        processAuthResponseCallCount++
        return processResultToReturn.also {
            if (it.isSuccess) {
                _state.value = true
                currentToken = "fake-token"
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        logoutCallCount++
        return logoutResultToReturn.also {
            if (it.isSuccess) {
                _state.value = false
                currentToken = null
            }
        }
    }

    override suspend fun refreshIfNeeded(): Result<String> =
        currentToken?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Not authenticated"))
}

/**
 * Ручной Fake для [AuthIntentProvider].
 */
class FakeAuthIntentProvider : AuthIntentProvider {
    var intentToReturn: Intent = Intent("fake.auth.tab")
    var shouldThrow: Exception? = null
    var createIntentCallCount: Int = 0
    var lastIdpHint: String? = null
    var lastPrompt: String? = null

    override suspend fun createAuthIntent(
        idpHint: String?,
        prompt: String?,
    ): Intent {
        createIntentCallCount++
        lastIdpHint = idpHint
        lastPrompt = prompt
        shouldThrow?.let { throw it }
        return intentToReturn
    }
}
