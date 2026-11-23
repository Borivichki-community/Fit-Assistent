package dev.andre.nutritionist.profile.grpc.security

import io.grpc.Context
import kotlin.coroutines.CoroutineContext

/**
 * Контекст аутентификации и авторизации gRPC запроса (T-021).
 *
 * Хранит идентификатор пользователя ([userId]), набор ролей ([roles])
 * и признак успешной аутентификации ([isAuthenticated]).
 * Пробрасывается через gRPC [Context] и доступен в Kotlin Coroutines.
 */
data class AuthContext(
    val userId: String?,
    val roles: Set<String> = emptySet(),
    val isAuthenticated: Boolean = false,
) {
    fun hasRole(role: String): Boolean = roles.contains(role)

    val isAdmin: Boolean
        get() = hasRole(ROLE_ADMIN) || hasRole(ROLE_SERVICE) || hasRole("admin")

    companion object {
        const val ROLE_USER = "ROLE_USER"
        const val ROLE_ADMIN = "ROLE_ADMIN"
        const val ROLE_SERVICE = "ROLE_SERVICE"

        val UNAUTHENTICATED = AuthContext(userId = null, roles = emptySet(), isAuthenticated = false)

        val AUTH_CONTEXT_KEY: Context.Key<AuthContext> =
            Context.key<AuthContext>("dev.andre.nutritionist.profile.grpc.security.AuthContext")
    }
}

/**
 * Получение текущего [AuthContext] из gRPC [Context.current].
 */
fun currentAuthContext(): AuthContext = AuthContext.AUTH_CONTEXT_KEY.get() ?: AuthContext.UNAUTHENTICATED

/**
 * Расширение для [CoroutineContext] для получения [AuthContext].
 */
val CoroutineContext.authContext: AuthContext
    get() = currentAuthContext()
