package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import io.grpc.Status
import io.grpc.StatusException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class ProfileGrpcService : ProfileServiceGrpcKt.ProfileServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(ProfileGrpcService::class.java)

    override suspend fun getProfile(request: GetProfileRequest): ProfileProto {
        val userId =
            request.userId.takeIf { it.isNotBlank() }
                ?: throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("user_id must not be blank"),
                )

        MDC.put("userId", userId)
        try {
            logger.info("getProfile called: userId={}", userId)

            // TODO [T-017]: реализовать через ProfileRepository (JPA + PostgreSQL)
            throw StatusException(
                Status.UNIMPLEMENTED.withDescription(
                    "GetProfile is not yet implemented — skeleton only",
                ),
            )
        } finally {
            MDC.remove("userId")
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto {
        val userId =
            request.userId.takeIf { it.isNotBlank() }
                ?: throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("user_id must not be blank"),
                )

        MDC.put("userId", userId)
        try {
            logger.info(
                "updateProfile called: userId={}, heightCm={}, weightKg={}",
                userId,
                request.heightCm,
                request.weightKg,
            )

            // TODO [T-017]: реализовать через ProfileRepository (JPA + PostgreSQL)
            throw StatusException(
                Status.UNIMPLEMENTED.withDescription(
                    "UpdateProfile is not yet implemented — skeleton only",
                ),
            )
        } finally {
            MDC.remove("userId")
        }
    }
}
