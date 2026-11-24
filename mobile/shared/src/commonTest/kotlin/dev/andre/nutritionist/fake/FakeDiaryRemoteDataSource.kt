package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.model.MealRecord

class FakeDiaryRemoteDataSource : DiaryRemoteDataSource {

    // mutable by design: test control
    val sentRecords = mutableListOf<MealRecord>()
    val receivedIdempotencyKeys = mutableListOf<String>()
    var shouldFail: Throwable? = null
    var addEntryCallCount: Int = 0

    override suspend fun addEntry(record: MealRecord): Result<MealRecord> {
        addEntryCallCount++
        receivedIdempotencyKeys.add(record.idempotencyKey.raw)
        val error = shouldFail
        if (error != null) {
            return Result.failure(error)
        }
        sentRecords.add(record)
        return Result.success(record)
    }

    override suspend fun getDailyEntries(date: String, timeZone: String): Result<List<MealRecord>> {
        val error = shouldFail
        if (error != null) {
            return Result.failure(error)
        }
        return Result.success(sentRecords.toList())
    }

    override suspend fun deleteMeal(entryId: String): Result<Unit> {
        val error = shouldFail
        if (error != null) {
            return Result.failure(error)
        }
        sentRecords.removeAll { it.id.raw == entryId }
        return Result.success(Unit)
    }

    fun reset() {
        sentRecords.clear()
        receivedIdempotencyKeys.clear()
        shouldFail = null
        addEntryCallCount = 0
    }
}
