package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FakeDiaryRepository : DiaryRepository {

    private val recordsFlow = MutableStateFlow<List<MealRecord>>(emptyList())

    // mutable by design: test control
    val recordsByUser = mutableMapOf<String, List<MealRecord>>()
    var addEntryCallCount: Int = 0
    var getRangeCallCount: Int = 0
    var lastGetRangeTargetUserId: UserId? = null
    var shouldFailAddEntry: Throwable? = null
    var shouldFailGetRange: Throwable? = null
    var failOnAddCallNumber: Int? = null

    val addedRecords: List<MealRecord>
        get() = recordsFlow.value

    override suspend fun addEntry(record: MealRecord): Result<MealRecord> {
        addEntryCallCount++
        val failCall = failOnAddCallNumber
        if (failCall != null && addEntryCallCount == failCall) {
            return Result.failure(shouldFailAddEntry ?: RuntimeException("Forced failure on call #$failCall"))
        }
        val error = shouldFailAddEntry
        if (failCall == null && error != null) {
            return Result.failure(error)
        }
        recordsFlow.value = recordsFlow.value + record
        return Result.success(record)
    }

    override fun observeDailyRecords(fromEpochMs: Long, toEpochMs: Long): Flow<List<MealRecord>> =
        recordsFlow.map { records ->
            records.filter { record ->
                val epochMs = record.recordedAt.toEpochMilliseconds()
                record.syncStatus != SyncStatus.DELETED_PENDING && epochMs in fromEpochMs..toEpochMs
            }
        }

    override suspend fun getDailyRecords(fromEpochMs: Long, toEpochMs: Long): Result<List<MealRecord>> {
        val filtered = recordsFlow.value.filter { record ->
            val epochMs = record.recordedAt.toEpochMilliseconds()
            record.syncStatus != SyncStatus.DELETED_PENDING && epochMs in fromEpochMs..toEpochMs
        }
        return Result.success(filtered)
    }

    override suspend fun getRange(
        fromIso: String,
        untilIso: String,
        targetUserId: UserId?,
        timeZone: String,
    ): Result<List<MealRecord>> {
        getRangeCallCount++
        lastGetRangeTargetUserId = targetUserId
        shouldFailGetRange?.let { return Result.failure(it) }

        val tz = runCatching { TimeZone.of(timeZone) }.getOrElse { TimeZone.UTC }
        val fromDate = LocalDate.parse(fromIso)
        val untilDate = LocalDate.parse(untilIso)
        val source = if (targetUserId != null) {
            recordsByUser[targetUserId.raw].orEmpty()
        } else {
            recordsFlow.value
        }

        val filtered = source.filter { record ->
            val date = record.recordedAt.toLocalDateTime(tz).date
            record.syncStatus != SyncStatus.DELETED_PENDING && date in fromDate..untilDate
        }
        return Result.success(filtered)
    }

    override suspend fun getPendingRecords(): List<MealRecord> =
        recordsFlow.value.filter { it.syncStatus == SyncStatus.PENDING }

    override suspend fun syncPendingRecords(): Result<Int> {
        var syncedCount = 0
        recordsFlow.value = recordsFlow.value.mapNotNull { record ->
            when (record.syncStatus) {
                SyncStatus.PENDING -> {
                    syncedCount++
                    record.copy(syncStatus = SyncStatus.SYNCED)
                }
                SyncStatus.DELETED_PENDING -> {
                    syncedCount++
                    null
                }
                SyncStatus.SYNCED,
                SyncStatus.FAILED -> record
            }
        }
        return Result.success(syncedCount)
    }

    override suspend fun deleteEntry(id: MealRecordId): Result<Unit> {
        recordsFlow.value = recordsFlow.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    fun reset() {
        recordsFlow.value = emptyList()
        recordsByUser.clear()
        addEntryCallCount = 0
        getRangeCallCount = 0
        lastGetRangeTargetUserId = null
        shouldFailAddEntry = null
        shouldFailGetRange = null
        failOnAddCallNumber = null
    }
}
