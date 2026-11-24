package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DeleteMealResponse
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import dev.andre.nutritionist.data.mapper.DiaryDtoMapper.toAddEntryRequest
import dev.andre.nutritionist.data.mapper.DiaryDtoMapper.toDomain
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealRecord
import kotlinx.coroutines.withContext

interface DiaryGrpcServiceClient {
    suspend fun addEntry(request: AddEntryRequest, idempotencyKey: String? = null): AddEntryResponse
    suspend fun getDailyEntries(request: GetDailyEntriesRequest): GetDailyEntriesResponse
    suspend fun deleteMeal(request: DeleteMealRequest): DeleteMealResponse
}

class DiaryGrpcRemoteDataSource(
    private val client: DiaryGrpcServiceClient,
    private val dispatchers: CoroutineDispatchers,
) : DiaryRemoteDataSource {

    override suspend fun addEntry(record: MealRecord): Result<MealRecord> =
        withContext(dispatchers.io) {
            runCatching {
                val request = record.toAddEntryRequest()
                val response = client.addEntry(request, record.idempotencyKey.raw)
                response.entry.toDomain()
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
        }

    override suspend fun getDailyEntries(date: String, timeZone: String): Result<List<MealRecord>> =
        withContext(dispatchers.io) {
            runCatching {
                val request = GetDailyEntriesRequest(date = date, timeZone = timeZone)
                val response = client.getDailyEntries(request)
                response.entries.map { it.toDomain() }
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
        }

    override suspend fun deleteMeal(entryId: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val request = DeleteMealRequest(entryId = entryId)
                client.deleteMeal(request)
                Unit
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
        }
}
