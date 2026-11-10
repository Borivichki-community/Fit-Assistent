package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ProfileGrpcServiceTest {

    private val service = ProfileGrpcService()

    @Test
    fun `getProfile throws INVALID_ARGUMENT when userId is blank`() {
        val request = GetProfileRequest.newBuilder().setUserId("   ").build()

        val exception = assertThrows(StatusException::class.java) {
            runBlocking {
                service.getProfile(request)
            }
        }

        assertEquals(Status.INVALID_ARGUMENT.code, exception.status.code)
        assertEquals("user_id must not be blank", exception.status.description)
    }

    @Test
    fun `getProfile throws UNIMPLEMENTED when userId is valid`() {
        val request = GetProfileRequest.newBuilder().setUserId("valid-user-id").build()

        val exception = assertThrows(StatusException::class.java) {
            runBlocking {
                service.getProfile(request)
            }
        }

        assertEquals(Status.UNIMPLEMENTED.code, exception.status.code)
        assertEquals("GetProfile is not yet implemented — skeleton only", exception.status.description)
    }

    @Test
    fun `updateProfile throws INVALID_ARGUMENT when userId is blank`() {
        val request = UpdateProfileRequest.newBuilder().setUserId("").build()

        val exception = assertThrows(StatusException::class.java) {
            runBlocking {
                service.updateProfile(request)
            }
        }

        assertEquals(Status.INVALID_ARGUMENT.code, exception.status.code)
        assertEquals("user_id must not be blank", exception.status.description)
    }

    @Test
    fun `updateProfile throws UNIMPLEMENTED when userId is valid`() {
        val request = UpdateProfileRequest.newBuilder().setUserId("valid-user-id").build()

        val exception = assertThrows(StatusException::class.java) {
            runBlocking {
                service.updateProfile(request)
            }
        }

        assertEquals(Status.UNIMPLEMENTED.code, exception.status.code)
        assertEquals("UpdateProfile is not yet implemented — skeleton only", exception.status.description)
    }
}
