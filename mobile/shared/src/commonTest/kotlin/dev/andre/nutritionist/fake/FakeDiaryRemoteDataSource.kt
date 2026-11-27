package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.UserId

class FakeDiaryRemoteDataSource : DiaryRemoteDataSource {

    // mutable by design: test control
    val sentRecords = mutableListOf<MealRecord>()
    val receivedIdempotencyKeys = mutableListOf<String>()
    val deletedEntryIds = mutableListOf<String>()
    val recordsByTargetUser = mutableMapOf<String, List<MealRecord>>()
    var shouldFail: Throwable? = null
    var shouldFailDelete: Throwable? = null
    var addEntryCallCount: Int = 0
    var getRangeCallCount: Int = 0
    var deleteMealCallCount: Int = 0
    var lastDailyEntriesTargetUserId: UserId? = null
    var lastGetRangeTargetUserId: UserId? = null
    var lastGetRangeFromIso: String? = null
    var lastGetRangeUntilIso: String? = null

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

    override suspend fun getDailyEntries(
        date: String,
        timeZone: String,
        targetUserId: UserId?,
    ): Result<List<MealRecord>> {
        lastDailyEntriesTargetUserId = targetUserId
        val error = shouldFail
        if (error != null) {
            return Result.failure(error)
        }
        val list = if (targetUserId != null) {
            recordsByTargetUser[targetUserId.raw].orEmpty()
        } else {
            sentRecords.toList()
        }
        return Result.success(list)
    }

    override suspend fun getRange(
        fromIso: String,
        untilIso: String,
        timeZone: String,
        targetUserId: UserId?,
    ): Result<List<MealRecord>> {
        getRangeCallCount++
        lastGetRangeFromIso = fromIso
        lastGetRangeUntilIso = untilIso
        lastGetRangeTargetUserId = targetUserId
        val error = shouldFail
        if (error != null) {
            return Result.failure(error)
        }
        val list = if (targetUserId != null) {
            recordsByTargetUser[targetUserId.raw].orEmpty()
        } else {
            sentRecords.toList()
        }
        return Result.success(list)
    }

    override suspend fun deleteMeal(entryId: String): Result<Unit> {
        deleteMealCallCount++
        val error = shouldFailDelete ?: shouldFail
        if (error != null) {
            return Result.failure(error)
        }
        deletedEntryIds.add(entryId)
        sentRecords.removeAll { it.id.raw == entryId }
        return Result.success(Unit)
    }

    fun reset() {
        sentRecords.clear()
        receivedIdempotencyKeys.clear()
        deletedEntryIds.clear()
        recordsByTargetUser.clear()
        shouldFail = null
        shouldFailDelete = null
        addEntryCallCount = 0
        getRangeCallCount = 0
        deleteMealCallCount = 0
        lastDailyEntriesTargetUserId = null
        lastGetRangeTargetUserId = null
        lastGetRangeFromIso = null
        lastGetRangeUntilIso = null
    }
}
