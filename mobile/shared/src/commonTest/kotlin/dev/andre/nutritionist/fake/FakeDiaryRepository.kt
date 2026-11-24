package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDiaryRepository : DiaryRepository {

    private val recordsFlow = MutableStateFlow<List<MealRecord>>(emptyList())

    // mutable by design: test control
    var addEntryCallCount: Int = 0
    var shouldFailAddEntry: Throwable? = null
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
                epochMs in fromEpochMs..toEpochMs
            }
        }

    override suspend fun getDailyRecords(fromEpochMs: Long, toEpochMs: Long): Result<List<MealRecord>> {
        val filtered = recordsFlow.value.filter { record ->
            val epochMs = record.recordedAt.toEpochMilliseconds()
            epochMs in fromEpochMs..toEpochMs
        }
        return Result.success(filtered)
    }

    override suspend fun getPendingRecords(): List<MealRecord> =
        recordsFlow.value.filter { it.syncStatus == SyncStatus.PENDING }

    override suspend fun syncPendingRecords(): Result<Int> {
        var syncedCount = 0
        recordsFlow.value = recordsFlow.value.map { record ->
            if (record.syncStatus == SyncStatus.PENDING) {
                syncedCount++
                record.copy(syncStatus = SyncStatus.SYNCED)
            } else {
                record
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
        addEntryCallCount = 0
        shouldFailAddEntry = null
        failOnAddCallNumber = null
    }
}
