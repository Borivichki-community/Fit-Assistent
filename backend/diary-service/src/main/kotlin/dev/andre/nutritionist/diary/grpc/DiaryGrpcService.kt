package dev.andre.nutritionist.diary.grpc

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DeleteMealResponse
import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import com.budgetntr.proto.diary.v1.DiaryServiceGrpcKt
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.diary.v1.GetRangeResponse
import dev.andre.nutritionist.diary.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyService
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyService.IdempotencyConflictException
import dev.andre.nutritionist.diary.grpc.idempotency.IdempotencyStore.IdempotencyScope
import dev.andre.nutritionist.diary.grpc.interceptor.DiaryRequestContextInterceptor
import dev.andre.nutritionist.diary.grpc.mapper.DiaryGrpcMapper
import dev.andre.nutritionist.diary.grpc.security.DiaryAuthContext
import dev.andre.nutritionist.diary.grpc.validation.DiaryRequestValidator
import dev.andre.nutritionist.diary.service.CoachAccessCacheService
import dev.andre.nutritionist.diary.service.DiaryService
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.GrpcContextElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.grpc.server.service.GrpcService
import java.time.DateTimeException
import java.util.UUID

@GrpcService
class DiaryGrpcService(
    private val diaryService: DiaryService,
    private val coachAccessCacheService: CoachAccessCacheService,
    private val idempotency: IdempotencyService,
    private val mapper: DiaryGrpcMapper,
    private val validator: DiaryRequestValidator,
    private val dispatchers: CoroutineDispatchers,
) : DiaryServiceGrpcKt.DiaryServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(DiaryGrpcService::class.java)

    override suspend fun addEntry(request: AddEntryRequest): AddEntryResponse {
        val userId = DiaryRequestContextInterceptor.currentUserId()
        val key = DiaryRequestContextInterceptor.currentKey()
        val traceparent = DiaryRequestContextInterceptor.currentTraceparent()
        return execute(userId, traceparent) {
            validator.validate(request)
            val scope = IdempotencyScope(userId, DiaryServiceGrpc.getAddEntryMethod().fullMethodName, key)
            val response =
                idempotency.execute(scope, request.toByteArray()) {
                    val entry = diaryService.addEntry(mapper.toCommand(request, userId, key), traceparent)
                    AddEntryResponse
                        .newBuilder()
                        .setEntry(mapper.toProto(entry))
                        .build()
                        .toByteArray()
                }
            AddEntryResponse.parseFrom(response)
        }
    }

    override suspend fun deleteMeal(request: DeleteMealRequest): DeleteMealResponse {
        val userId = DiaryRequestContextInterceptor.currentUserId()
        val key = DiaryRequestContextInterceptor.currentKey()
        val traceparent = DiaryRequestContextInterceptor.currentTraceparent()
        return execute(userId, traceparent) {
            validator.validate(request)
            val scope = IdempotencyScope(userId, DiaryServiceGrpc.getDeleteMealMethod().fullMethodName, key)
            val response =
                idempotency.execute(scope, request.toByteArray()) {
                    diaryService.deleteMeal(userId, DiaryEntryId(UUID.fromString(request.entryId)))
                    DeleteMealResponse.getDefaultInstance().toByteArray()
                }
            DeleteMealResponse.parseFrom(response)
        }
    }

    override suspend fun getDailyEntries(request: GetDailyEntriesRequest): GetDailyEntriesResponse {
        val currentUserId = DiaryRequestContextInterceptor.currentUserId()
        val authContext = DiaryRequestContextInterceptor.currentAuthContext()
        val traceparent = DiaryRequestContextInterceptor.currentTraceparent()
        return execute(currentUserId, traceparent) {
            val targetUserId = resolveTargetUserId(currentUserId, request.hasTargetUserId(), request.targetUserId)
            authorizeTargetDiaryRead(currentUserId, targetUserId, authContext)
            val (from, until) = mapper.dateBounds(request.date, request.date, request.timeZone)
            val day = diaryService.getDay(targetUserId, from, until)
            GetDailyEntriesResponse
                .newBuilder()
                .addAllEntries(day.entries.map(mapper::toProto))
                .setDailyTotal(mapper.toProto(day.dailyTotal))
                .addAllMealTotals(day.mealTotals.map(mapper::toProto))
                .build()
        }
    }

    override suspend fun getRange(request: GetRangeRequest): GetRangeResponse {
        val currentUserId = DiaryRequestContextInterceptor.currentUserId()
        val authContext = DiaryRequestContextInterceptor.currentAuthContext()
        val traceparent = DiaryRequestContextInterceptor.currentTraceparent()
        return execute(currentUserId, traceparent) {
            val targetUserId = resolveTargetUserId(currentUserId, request.hasTargetUserId(), request.targetUserId)
            authorizeTargetDiaryRead(currentUserId, targetUserId, authContext)
            val (from, until) = mapper.dateBounds(request.startDate, request.endDate, request.timeZone)
            GetRangeResponse
                .newBuilder()
                .addAllEntries(diaryService.getRange(targetUserId, from, until).map(mapper::toProto))
                .build()
        }
    }

    private fun resolveTargetUserId(
        currentUserId: UserId,
        hasTargetUserId: Boolean,
        rawTargetUserId: String,
    ): UserId {
        if (!hasTargetUserId || rawTargetUserId.isBlank()) {
            return currentUserId
        }
        val normalized = rawTargetUserId.trim()
        require(normalized.length <= 64 && !normalized.contains(':') && !normalized.any(Char::isWhitespace)) {
            "Invalid target_user_id"
        }
        return UserId(normalized)
    }

    private suspend fun authorizeTargetDiaryRead(
        currentUserId: UserId,
        targetUserId: UserId,
        authContext: DiaryAuthContext,
    ) {
        if (targetUserId == currentUserId || authContext.isAdmin) {
            return
        }
        val hasAccess = coachAccessCacheService.hasCoachAccess(currentUserId.raw, targetUserId.raw)
        if (!hasAccess) {
            throw Status.PERMISSION_DENIED
                .withDescription("Coach access denied for target user diary")
                .asRuntimeException()
        }
    }

    private suspend fun <T> execute(
        userId: UserId,
        traceparent: String?,
        operation: suspend () -> T,
    ): T {
        val mdc =
            buildMap {
                put("userId", userId.raw)
                traceparent?.let { put("traceparent", it) }
            }
        return withContext(dispatchers.io + GrpcContextElement.current() + MDCContext(mdc)) {
            try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: StatusException) {
                throw error
            } catch (error: StatusRuntimeException) {
                throw error
            } catch (error: IdempotencyConflictException) {
                throw Status.ALREADY_EXISTS.withDescription(error.message).asRuntimeException()
            } catch (error: DataIntegrityViolationException) {
                throw Status.ALREADY_EXISTS.withDescription("Diary entry or creation key already exists").asRuntimeException()
            } catch (error: NoSuchElementException) {
                throw Status.NOT_FOUND.withDescription(error.message).asRuntimeException()
            } catch (error: ArithmeticException) {
                throw Status.RESOURCE_EXHAUSTED.withDescription("Nutrition total exceeds supported range").asRuntimeException()
            } catch (error: IllegalArgumentException) {
                throw Status.INVALID_ARGUMENT.withDescription(error.message).asRuntimeException()
            } catch (error: DateTimeException) {
                throw Status.INVALID_ARGUMENT.withDescription("Invalid date or time zone").asRuntimeException()
            } catch (error: Exception) {
                logger.error("Diary request failed", error)
                throw Status.INTERNAL.withDescription("Diary request failed").asRuntimeException()
            }
        }
    }
}
