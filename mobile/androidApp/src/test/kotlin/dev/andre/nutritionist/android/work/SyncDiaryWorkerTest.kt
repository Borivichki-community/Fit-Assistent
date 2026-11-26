package dev.andre.nutritionist.android.work

import androidx.work.BackoffPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDiaryWorkerTest {

    private fun testDispatchers(dispatcher: CoroutineDispatcher): CoroutineDispatchers =
        object : CoroutineDispatchers {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val unconfined: CoroutineDispatcher = dispatcher
        }

    @Test
    fun `buildWorkRequest configures CONNECTED network and EXPONENTIAL 10s backoff`() {
        val request = SyncDiaryWorker.buildWorkRequest()
        val spec = request.workSpec

        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals(10_000L, spec.backoffDelayDuration)
    }

    @Test
    fun `syncPendingEntries returns success immediately when no PENDING records exist`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val dao = LocalFakeMealRecordDao()
            val remote = LocalFakeDiaryRemoteDataSource()

            val result = SyncDiaryWorker.syncPendingEntries(
                mealRecordDao = dao,
                diaryRemoteDataSource = remote,
                dispatchers = testDispatchers(dispatcher),
                runAttemptCount = 0,
            )

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(0, remote.sentRecords.size)
        }
    }

    @Test
    fun `syncPendingEntries sends PENDING records with same idempotencyKey and marks SYNCED`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val dao = LocalFakeMealRecordDao()
            dao.insert(pendingEntity(id = "rec-1", idempotencyKey = "idem-1"))
            dao.insert(pendingEntity(id = "rec-2", idempotencyKey = "idem-2"))
            val remote = LocalFakeDiaryRemoteDataSource()

            val result = SyncDiaryWorker.syncPendingEntries(
                mealRecordDao = dao,
                diaryRemoteDataSource = remote,
                dispatchers = testDispatchers(dispatcher),
                runAttemptCount = 0,
            )

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(listOf("idem-1", "idem-2"), remote.receivedKeys)
            assertEquals(SyncStatus.SYNCED.name, dao.getById("rec-1")?.syncStatus)
            assertEquals(SyncStatus.SYNCED.name, dao.getById("rec-2")?.syncStatus)
        }
    }

    @Test
    fun `syncPendingEntries returns retry on network failure below MAX_RETRIES`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val dao = LocalFakeMealRecordDao()
            dao.insert(pendingEntity(id = "rec-1", idempotencyKey = "idem-1"))
            val remote = LocalFakeDiaryRemoteDataSource().apply {
                shouldFail = RuntimeException("Timeout")
            }

            val result = SyncDiaryWorker.syncPendingEntries(
                mealRecordDao = dao,
                diaryRemoteDataSource = remote,
                dispatchers = testDispatchers(dispatcher),
                runAttemptCount = SyncDiaryWorker.MAX_RETRIES - 1,
            )

            assertEquals(ListenableWorker.Result.retry(), result)
            assertEquals(SyncStatus.PENDING.name, dao.getById("rec-1")?.syncStatus)
        }
    }

    @Test
    fun `syncPendingEntries marks FAILED and returns failure when runAttemptCount reaches MAX_RETRIES`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val dao = LocalFakeMealRecordDao()
            dao.insert(pendingEntity(id = "rec-1", idempotencyKey = "idem-1"))
            val remote = LocalFakeDiaryRemoteDataSource().apply {
                shouldFail = RuntimeException("Server error")
            }

            val result = SyncDiaryWorker.syncPendingEntries(
                mealRecordDao = dao,
                diaryRemoteDataSource = remote,
                dispatchers = testDispatchers(dispatcher),
                runAttemptCount = SyncDiaryWorker.MAX_RETRIES,
            )

            assertEquals(ListenableWorker.Result.failure(), result)
            assertEquals(SyncStatus.FAILED.name, dao.getById("rec-1")?.syncStatus)
        }
    }

    @Test
    fun `syncPendingEntries processes DELETED_PENDING tombstones via deleteMeal and removes permanently`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val dao = LocalFakeMealRecordDao()
            dao.insert(
                pendingEntity(id = "del-1", idempotencyKey = "idem-del-1").copy(
                    syncStatus = SyncStatus.DELETED_PENDING.name,
                    isDeleted = true,
                ),
            )
            val remote = LocalFakeDiaryRemoteDataSource()

            val result = SyncDiaryWorker.syncPendingEntries(
                mealRecordDao = dao,
                diaryRemoteDataSource = remote,
                dispatchers = testDispatchers(dispatcher),
                runAttemptCount = 0,
            )

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(listOf("del-1"), remote.deletedIds)
            assertEquals(null, dao.getById("del-1"))
        }
    }

    private fun pendingEntity(id: String, idempotencyKey: String) = MealRecordEntity(
        id = id,
        idempotencyKey = idempotencyKey,
        syncStatus = SyncStatus.PENDING.name,
        mealType = "BREAKFAST",
        recordedAtEpoch = 1_700_000_000_000L,
        sourceType = "PRODUCT",
        sourceId = "prod-1",
        nameSnapshot = "Овсянка",
        weightGrams = 150.0,
        calories = 200.0,
        proteins = 7.0,
        fats = 4.0,
        carbs = 34.0,
        costAmountMinor = 4_000L,
        costCurrency = "RUB",
    )

    private class LocalFakeMealRecordDao : MealRecordDao {
        private val items = linkedMapOf<String, MealRecordEntity>()
        private val flow = MutableStateFlow<List<MealRecordEntity>>(emptyList())

        override suspend fun insert(record: MealRecordEntity) {
            items[record.id] = record
            flow.value = items.values.toList()
        }

        override suspend fun countAllIncludingDeleted(): Int = items.size

        override suspend fun getById(id: String): MealRecordEntity? = items[id]

        override suspend fun getPendingRecords(): List<MealRecordEntity> =
            items.values.filter { !it.isDeleted && it.syncStatus == SyncStatus.PENDING.name }

        override suspend fun getPendingDeletedRecords(): List<MealRecordEntity> =
            items.values.filter { it.isDeleted && it.syncStatus == SyncStatus.DELETED_PENDING.name }

        override suspend fun updateSyncStatus(id: String, status: String) {
            items[id]?.let { existing ->
                items[id] = existing.copy(syncStatus = status)
                flow.value = items.values.toList()
            }
        }

        override suspend fun markAsDeleted(id: String, status: String) {
            items[id]?.let { existing ->
                items[id] = existing.copy(isDeleted = true, syncStatus = status)
                flow.value = items.values.toList()
            }
        }

        override fun getDailyRecordsFlow(fromEpoch: Long, toEpoch: Long): Flow<List<MealRecordEntity>> =
            flow.map { list -> list.filter { !it.isDeleted && it.recordedAtEpoch in fromEpoch..toEpoch } }

        override suspend fun getDailyRecords(fromEpoch: Long, toEpoch: Long): List<MealRecordEntity> =
            items.values.filter { !it.isDeleted && it.recordedAtEpoch in fromEpoch..toEpoch }

        override suspend fun deletePermanently(id: String) {
            items.remove(id)
            flow.value = items.values.toList()
        }

        override suspend fun deleteById(id: String) {
            deletePermanently(id)
        }
    }

    private class LocalFakeDiaryRemoteDataSource : DiaryRemoteDataSource {
        var shouldFail: Throwable? = null
        val sentRecords = mutableListOf<MealRecord>()
        val receivedKeys = mutableListOf<String>()
        val deletedIds = mutableListOf<String>()

        override suspend fun addEntry(record: MealRecord): Result<MealRecord> {
            receivedKeys.add(record.idempotencyKey.raw)
            shouldFail?.let { return Result.failure(it) }
            sentRecords.add(record)
            return Result.success(record)
        }

        override suspend fun getDailyEntries(
            date: String,
            timeZone: String,
            targetUserId: UserId?,
        ): Result<List<MealRecord>> = Result.success(sentRecords.toList())

        override suspend fun getRange(
            fromIso: String,
            untilIso: String,
            timeZone: String,
            targetUserId: UserId?,
        ): Result<List<MealRecord>> = Result.success(sentRecords.toList())

        override suspend fun deleteMeal(entryId: String): Result<Unit> {
            shouldFail?.let { return Result.failure(it) }
            deletedIds.add(entryId)
            return Result.success(Unit)
        }
    }
}
