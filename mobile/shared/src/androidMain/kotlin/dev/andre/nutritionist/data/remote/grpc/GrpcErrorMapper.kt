package dev.andre.nutritionist.data.remote.grpc

import dev.andre.nutritionist.domain.error.DomainError
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException

/**
 * Маппер gRPC-ошибок (StatusException / StatusRuntimeException) в типизированные доменные ошибки DomainError.
 * Реализует исчерпывающий анализ Status.Code без ветки else.
 */
object GrpcErrorMapper {

    fun map(throwable: Throwable): DomainError {
        val (code, description) = when (throwable) {
            is StatusException -> throwable.status.code to (throwable.status.description ?: throwable.message ?: "gRPC error")
            is StatusRuntimeException -> throwable.status.code to (throwable.status.description ?: throwable.message ?: "gRPC error")
            else -> return DomainError.Unknown(throwable.message ?: "Unknown error", throwable)
        }

        return when (code) {
            Status.Code.UNAUTHENTICATED -> DomainError.Unauthorized(description)
            Status.Code.PERMISSION_DENIED -> DomainError.Forbidden(description)
            Status.Code.NOT_FOUND -> DomainError.NotFound(entity = "Resource", id = description)
            Status.Code.ALREADY_EXISTS -> DomainError.Conflict(description)
            Status.Code.UNAVAILABLE,
            Status.Code.DEADLINE_EXCEEDED,
            -> DomainError.NetworkUnavailable(description, throwable)
            Status.Code.ABORTED -> DomainError.ConcurrentModification(description)
            Status.Code.INVALID_ARGUMENT -> DomainError.ValidationError(field = "argument", reason = description)
            Status.Code.FAILED_PRECONDITION -> DomainError.ValidationError(field = "precondition", reason = description)
            Status.Code.RESOURCE_EXHAUSTED -> DomainError.BudgetExceededError(requiredMinor = 0L, maximumMinor = 0L)
            Status.Code.CANCELLED -> DomainError.NetworkUnavailable(details = "Request was cancelled", cause = throwable)
            Status.Code.OK -> DomainError.Unknown("Status OK treated as error")
            Status.Code.UNKNOWN,
            Status.Code.INTERNAL,
            Status.Code.DATA_LOSS,
            Status.Code.UNIMPLEMENTED,
            Status.Code.OUT_OF_RANGE,
            -> DomainError.Unknown(description, throwable)
        }
    }
}
