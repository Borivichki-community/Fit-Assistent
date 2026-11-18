package dev.andre.nutritionist.profile.grpc

import com.budgetntr.proto.profile.v1.DailyCaloriesBalanceResponse
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.profile.domain.mapper.toCommand
import dev.andre.nutritionist.profile.domain.mapper.toProto
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.grpc.idempotency.IdempotencyServerInterceptor
import dev.andre.nutritionist.profile.grpc.interceptor.DailyCaloriesIdentityInterceptor
import dev.andre.nutritionist.profile.grpc.interceptor.GrpcExceptionInterceptor
import dev.andre.nutritionist.profile.grpc.validation.ProfileRequestValidator
import dev.andre.nutritionist.profile.service.DailyCaloriesService
import dev.andre.nutritionist.profile.service.ProfileService
import io.grpc.Status
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import com.budgetntr.proto.profile.v1.GetDailyCaloriesBalanceRequest as DailyCaloriesRequest

@GrpcService(interceptors = [GrpcExceptionInterceptor::class, IdempotencyServerInterceptor::class])
class ProfileGrpcService(
    private val profileService: ProfileService,
    private val validator: ProfileRequestValidator,
    private val dispatchers: CoroutineDispatchers,
    private val dailyCalories: DailyCaloriesService,
) : ProfileServiceGrpcKt.ProfileServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(ProfileGrpcService::class.java)

    override suspend fun getDailyCaloriesBalance(request: DailyCaloriesRequest): DailyCaloriesBalanceResponse {
        val identity =
            DailyCaloriesIdentityInterceptor.USER_ID.get()
                ?: throw Status.UNAUTHENTICATED.asRuntimeException()
        val userId =
            runCatching { UUID.fromString(request.userId) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid user_id").asRuntimeException() }
        val trustedId =
            runCatching { UUID.fromString(identity) }
                .getOrElse { throw Status.UNAUTHENTICATED.asRuntimeException() }
        if (userId != trustedId) throw Status.PERMISSION_DENIED.asRuntimeException()
        val date =
            runCatching { LocalDate.parse(request.date) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid ISO date").asRuntimeException() }
        val zone =
            runCatching { ZoneId.of(request.timeZone) }
                .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("Invalid time_zone").asRuntimeException() }
        return withContext(MDCContext(mapOf("userId" to userId.toString())) + dispatchers.io) {
            val balance = dailyCalories.getBalance(UserId(userId.toString()), date, zone)
            DailyCaloriesBalanceResponse
                .newBuilder()
                .setDate(date.toString())
                .setTimeZone(zone.id)
                .setTargetCalories(balance.targetCalories)
                .setConsumedCalories(balance.consumedCalories)
                .setRemainingCalories(balance.remainingCalories)
                .build()
        }
    }

    override suspend fun getProfile(request: GetProfileRequest): ProfileProto {
        validator.validate(request)

        return withContext(MDCContext(mapOf("userId" to request.userId)) + dispatchers.io) {
            logger.info("getProfile called: userId={}", request.userId)

            val profile =
                profileService.getProfileByUserId(UserId(request.userId))
                    ?: throw NoSuchElementException("Profile not found")

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

            val command = request.toCommand()
            val profile = profileService.upsertProfile(command)

            logger.info("updateProfile completed successfully: id={}, userId={}", profile.id, profile.userId)
            profile.toProto()
        }
    }
}
