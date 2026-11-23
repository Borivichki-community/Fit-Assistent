package dev.andre.nutritionist.catalog.grpc.security

import io.grpc.Context
import kotlin.coroutines.CoroutineContext

/**
 * Контекст аутентификации и авторизации gRPC запроса для catalog-price-service.
 *
 * Хранит идентификатор пользователя ([userId]), набор ролей ([roles])
 * и признак успешной аутентификации ([isAuthenticated]).
 * Пробрасывается через gRPC [Context] и доступен в Kotlin Coroutines благодаря [io.grpc.kotlin.GrpcContextElement].
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
            Context.key<AuthContext>("dev.andre.nutritionist.catalog.grpc.security.AuthContext")
    }
}

/**
 * Получение текущего [AuthContext] из gRPC [Context.current].
 * При использовании [io.grpc.kotlin.GrpcContextElement] в контексте корутины
 * [Context.current] автоматически восстанавливается на любом потоке выполнения.
 */
fun currentAuthContext(): AuthContext = AuthContext.AUTH_CONTEXT_KEY.get() ?: AuthContext.UNAUTHENTICATED

val CoroutineContext.authContext: AuthContext
    get() = currentAuthContext()

/**
 * Приостанавливаемая функция для безопасного получения текущего [AuthContext] внутри корутины.
 */
suspend fun currentCoroutineAuthContext(): AuthContext = currentAuthContext()
