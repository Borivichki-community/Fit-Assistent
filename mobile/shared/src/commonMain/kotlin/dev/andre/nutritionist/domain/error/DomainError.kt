package dev.andre.nutritionist.domain.error

/**
 * Иерархия типизированных доменных ошибок приложения.
 * Запрещено пробрасывать сырые сетевые или БД-исключения в UI.
 */
sealed class DomainError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    data class ValidationError(val field: String, val reason: String) :
        DomainError("Validation failed for '$field': $reason")

    data class InvalidBudgetError(val details: String) :
        DomainError("Invalid budget: $details")

    data class BudgetExceededError(val requiredMinor: Long, val maximumMinor: Long) :
        DomainError("Budget exceeded: estimated $requiredMinor minor, maximum allowed is $maximumMinor minor")

    data class Unauthorized(val details: String = "User is not authenticated") :
        DomainError("Unauthorized: $details")

    data class Forbidden(val details: String = "Access denied") :
        DomainError("Forbidden: $details")

    data class NotFound(val entity: String, val id: String) :
        DomainError("$entity with id '$id' was not found")

    data class Conflict(val details: String) :
        DomainError("Conflict: $details")

    data class NetworkUnavailable(val details: String, override val cause: Throwable? = null) :
        DomainError("Network unavailable: $details", cause)

    data class ConcurrentModification(val details: String) :
        DomainError("Concurrent modification: $details")

    data class Unknown(val details: String, override val cause: Throwable? = null) :
        DomainError("Unknown error: $details", cause)
}
