package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DeleteMealResponse
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import com.budgetntr.proto.diary.v1.GetRangeRequest
import com.budgetntr.proto.diary.v1.GetRangeResponse
import dev.andre.nutritionist.data.mapper.DiaryDtoMapper.toAddEntryRequest
import dev.andre.nutritionist.data.mapper.DiaryDtoMapper.toDomain
import dev.andre.nutritionist.data.mapper.DiaryDtoMapper.toGetDailyEntriesRequest
import dev.andre.nutritionist.data.mapper.DiaryDtoMapper.toGetRangeRequest
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

interface DiaryGrpcServiceClient {
    suspend fun addEntry(request: AddEntryRequest, idempotencyKey: String? = null): AddEntryResponse
    suspend fun getDailyEntries(request: GetDailyEntriesRequest): GetDailyEntriesResponse
    suspend fun getRange(request: GetRangeRequest): GetRangeResponse
    suspend fun deleteMeal(request: DeleteMealRequest): DeleteMealResponse
}

class DiaryGrpcRemoteDataSource(
    private val client: DiaryGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
    private val errorMapper: (Throwable) -> Throwable = { it },
) : DiaryRemoteDataSource {

    override suspend fun addEntry(record: MealRecord): Result<MealRecord> =
        withContext(dispatchers.io) {
            runGrpc {
                val request = record.toAddEntryRequest()
                val response = client.addEntry(request, record.idempotencyKey.raw)
                response.entry.toDomain()
            }
        }

    override suspend fun getDailyEntries(
        date: String,
        timeZone: String,
        targetUserId: UserId?,
    ): Result<List<MealRecord>> =
        withContext(dispatchers.io) {
            runGrpc {
                val request = toGetDailyEntriesRequest(
                    date = date,
                    timeZone = timeZone,
                    targetUserId = targetUserId,
                )
                val response = client.getDailyEntries(request)
                response.entries.map { it.toDomain() }
            }
        }

    override suspend fun getRange(
        fromIso: String,
        untilIso: String,
        timeZone: String,
        targetUserId: UserId?,
    ): Result<List<MealRecord>> =
        withContext(dispatchers.io) {
            runGrpc {
                val request = toGetRangeRequest(
                    fromIso = fromIso,
                    untilIso = untilIso,
                    timeZone = timeZone,
                    targetUserId = targetUserId,
                )
                val response = client.getRange(request)
                response.toDomain()
            }
        }

    override suspend fun deleteMeal(entryId: String): Result<Unit> =
        withContext(dispatchers.io) {
            runGrpc {
                val request = DeleteMealRequest(entryId = entryId)
                client.deleteMeal(request)
                Unit
            }
        }

    private inline fun <T> runGrpc(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(errorMapper(t))
        }
}
