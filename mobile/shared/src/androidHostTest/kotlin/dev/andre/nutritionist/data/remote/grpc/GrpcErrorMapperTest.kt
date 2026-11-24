package dev.andre.nutritionist.data.remote.grpc

import dev.andre.nutritionist.domain.error.DomainError
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrpcErrorMapperTest {

    @Test
    fun mapUnauthenticatedToUnauthorized() {
        val ex = StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Invalid JWT token"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.Unauthorized)
        assertEquals("Invalid JWT token", (error as DomainError.Unauthorized).details)
    }

    @Test
    fun mapPermissionDeniedToForbidden() {
        val ex = StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("Not enough privileges"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.Forbidden)
        assertEquals("Not enough privileges", (error as DomainError.Forbidden).details)
    }

    @Test
    fun mapNotFoundToNotFound() {
        val ex = StatusRuntimeException(Status.NOT_FOUND.withDescription("Product 123"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.NotFound)
        assertEquals("Product 123", (error as DomainError.NotFound).id)
    }

    @Test
    fun mapAlreadyExistsToConflict() {
        val ex = StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Duplicate key"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.Conflict)
        assertEquals("Duplicate key", (error as DomainError.Conflict).details)
    }

    @Test
    fun mapUnavailableToNetworkUnavailable() {
        val ex = StatusRuntimeException(Status.UNAVAILABLE.withDescription("Connection refused"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.NetworkUnavailable)
        assertEquals("Connection refused", (error as DomainError.NetworkUnavailable).details)
    }

    @Test
    fun mapAbortedToConcurrentModification() {
        val ex = StatusRuntimeException(Status.ABORTED.withDescription("Optimistic lock failure"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.ConcurrentModification)
        assertEquals("Optimistic lock failure", (error as DomainError.ConcurrentModification).details)
    }

    @Test
    fun mapInvalidArgumentToValidationError() {
        val ex = StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid date format"))
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.ValidationError)
        assertEquals("argument", (error as DomainError.ValidationError).field)
        assertEquals("Invalid date format", (error as DomainError.ValidationError).reason)
    }

    @Test
    fun mapUnknownToUnknown() {
        val ex = RuntimeException("Unexpected IO error")
        val error = GrpcErrorMapper.map(ex)

        assertTrue(error is DomainError.Unknown)
        assertEquals("Unexpected IO error", (error as DomainError.Unknown).details)
    }
}
