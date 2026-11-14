package dev.andre.nutritionist.profile.grpc.interceptor

import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.protobuf.StatusProto
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import com.google.protobuf.Any as ProtoAny

class GrpcExceptionInterceptorTest {
    private val interceptor = GrpcExceptionInterceptor()

    @Test
    fun `translates NoSuchElementException to NOT_FOUND`() {
        val (status, _) = interceptor.translateThrowable(NoSuchElementException("User profile 'u1' not found"))
        assertEquals(Status.Code.NOT_FOUND, status.code)
        assertEquals("User profile 'u1' not found", status.description)
    }

    @Test
    fun `translates EntityNotFoundException to NOT_FOUND`() {
        val (status, _) = interceptor.translateThrowable(EntityNotFoundException("Entity 'u2' not found in database"))
        assertEquals(Status.Code.NOT_FOUND, status.code)
        assertEquals("Entity 'u2' not found in database", status.description)
    }

    @Test
    fun `translates IllegalArgumentException to INVALID_ARGUMENT`() {
        val (status, _) = interceptor.translateThrowable(IllegalArgumentException("Height must be positive"))
        assertEquals(Status.Code.INVALID_ARGUMENT, status.code)
        assertEquals("Height must be positive", status.description)
    }

    @Test
    fun `translates IllegalStateException to INVALID_ARGUMENT`() {
        val (status, _) = interceptor.translateThrowable(IllegalStateException("Invalid state for calculation"))
        assertEquals(Status.Code.INVALID_ARGUMENT, status.code)
        assertEquals("Invalid state for calculation", status.description)
    }

    @Test
    fun `translates DataIntegrityViolationException to ALREADY_EXISTS`() {
        val (status, _) =
            interceptor.translateThrowable(
                DataIntegrityViolationException("duplicate key value violates unique constraint uq_profiles_user_id"),
            )
        assertEquals(Status.Code.ALREADY_EXISTS, status.code)
        assertEquals("Resource conflict or unique constraint violation", status.description)
        assertFalse(status.description?.contains("duplicate key") == true, "SQL details must not leak")
    }

    @Test
    fun `passes through StatusException without distortion`() {
        val badRequest =
            BadRequest
                .newBuilder()
                .addFieldViolations(
                    BadRequest.FieldViolation
                        .newBuilder()
                        .setField("weight_kg")
                        .setDescription("must be between 20 and 500")
                        .build(),
                ).build()
        val originalStatusProto =
            com.google.rpc.Status
                .newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Validation error")
                .addDetails(ProtoAny.pack(badRequest))
                .build()
        val originalException = StatusProto.toStatusException(originalStatusProto)

        val (status, trailers) = interceptor.translateThrowable(originalException)
        assertEquals(Status.Code.INVALID_ARGUMENT, status.code)
        assertTrue(trailers.keys().isNotEmpty(), "Trailers with StatusProto details must be preserved")
    }

    @Test
    fun `passes through StatusRuntimeException without distortion`() {
        val original = Status.PERMISSION_DENIED.withDescription("Forbidden").asRuntimeException()
        val (status, _) = interceptor.translateThrowable(original)
        assertEquals(Status.Code.PERMISSION_DENIED, status.code)
        assertEquals("Forbidden", status.description)
    }

    @Test
    fun `translates unexpected Throwable to INTERNAL and hides internal SQL details`() {
        val sensitiveSqlError = RuntimeException("FATAL: connection to server at '10.0.0.5', port 5432 failed: timeout")
        val (status, _) = interceptor.translateThrowable(sensitiveSqlError)

        assertEquals(Status.Code.INTERNAL, status.code)
        assertEquals("Internal server error", status.description)
        assertFalse(status.description?.contains("10.0.0.5") == true, "Internal IP must not leak")
        assertFalse(status.description?.contains("5432") == true, "Internal port must not leak")
    }

    @Test
    fun `unwraps nested exceptions down to known domain exception`() {
        val root = NoSuchElementException("Target user missing")
        val wrapped = RuntimeException("Wrapper layer 1", RuntimeException("Wrapper layer 2", root))

        val (status, _) = interceptor.translateThrowable(wrapped)
        assertEquals(Status.Code.NOT_FOUND, status.code)
        assertEquals("Target user missing", status.description)
    }
}
