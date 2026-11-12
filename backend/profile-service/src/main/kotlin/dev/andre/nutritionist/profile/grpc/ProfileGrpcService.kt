package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.profile.domain.mapper.toProto
import dev.andre.nutritionist.profile.grpc.validation.ProfileRequestValidator
import dev.andre.nutritionist.profile.service.ProfileService
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class ProfileGrpcService(
    private val profileService: ProfileService,
    private val validator: ProfileRequestValidator,
    private val dispatchers: CoroutineDispatchers,
) : ProfileServiceGrpcKt.ProfileServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(ProfileGrpcService::class.java)

    override suspend fun getProfile(request: GetProfileRequest): ProfileProto {
        validator.validate(request)

        return withContext(MDCContext(mapOf("userId" to request.userId)) + dispatchers.io) {
            logger.info("getProfile called: userId={}", request.userId)

            val profile =
                profileService.getProfileByUserId(request.userId)
                    ?: throw StatusException(
                        Status.NOT_FOUND.withDescription("Profile for user_id '${request.userId}' not found"),
                    )

            logger.info("getProfile returning profile: id={}, userId={}", profile.id, profile.userId)
            profile.toProto()
        }
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto {
        validator.validate(request)

        return withContext(MDCContext(mapOf("userId" to request.userId)) + dispatchers.io) {
            logger.info(
                "updateProfile called: userId={}, heightCm={}, weightKg={}, weeklyBudget={}",
                request.userId,
                request.heightCm,
                request.weightKg,
                request.weeklyBudget,
            )

            val profile = profileService.upsertProfile(request)

            logger.info("updateProfile completed successfully: id={}, userId={}", profile.id, profile.userId)
            profile.toProto()
        }
    }
}
