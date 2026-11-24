package dev.andre.nutritionist.diary.grpc.security

import dev.andre.nutritionist.diary.domain.model.UserId
import io.grpc.Context

data class DiaryAuthContext(
    val userId: UserId?,
    val roles: Set<String> = emptySet(),
    val isAuthenticated: Boolean = false,
) {
    fun hasRole(role: String): Boolean = roles.contains(role)

    val isAdmin: Boolean
        get() = hasRole(ROLE_ADMIN) || hasRole(ROLE_SERVICE) || hasRole("admin")

    companion object {
        const val ROLE_USER = "ROLE_USER"
        const val ROLE_COACH = "ROLE_COACH"
        const val ROLE_ADMIN = "ROLE_ADMIN"
        const val ROLE_SERVICE = "ROLE_SERVICE"

        val UNAUTHENTICATED = DiaryAuthContext(userId = null, roles = emptySet(), isAuthenticated = false)

        val AUTH_CONTEXT_KEY: Context.Key<DiaryAuthContext> =
            Context.key<DiaryAuthContext>("dev.andre.nutritionist.diary.grpc.security.DiaryAuthContext")
    }
}
