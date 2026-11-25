package dev.andre.nutritionist.data.repository

import app.cash.turbine.test
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeDiaryRemoteDataSource
import dev.andre.nutritionist.fake.FakeMealRecordDao
import dev.andre.nutritionist.fake.FakeSyncScheduler
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DiaryTombstoneSyncTest {

    private lateinit var dao: FakeMealRecordDao
    private lateinit var remoteDataSource: FakeDiaryRemoteDataSource
    private lateinit var syncScheduler: FakeSyncScheduler
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var repository: DiaryRepositoryImpl

    @BeforeTest
    fun setUp() {
        dao = FakeMealRecordDao()
        remoteDataSource = FakeDiaryRemoteDataSource()
        syncScheduler = FakeSyncScheduler()
        dispatchers = TestCoroutineDispatchers()
        repository = DiaryRepositoryImpl(
            dao = dao,
            remoteDataSource = remoteDataSource,
            dispatchers = dispatchers,
            syncScheduler = syncScheduler,
        )
    }

    @Test
    fun `deleteEntry online hides record from Flow immediately and deletes permanently after remote success`() = runTest {
        val fromEpoch = 1700000000000L
        val toEpoch = 1700086400000L
        val record = createRecord(id = "rec-del-online", timestampMs = 1700005000000L)
        repository.addEntry(record)

        repository.observeDailyRecords(fromEpoch, toEpoch).test {
            assertEquals(listOf("rec-del-online"), awaitItem().map { it.id.raw })

            val deleteResult = repository.deleteEntry(record.id)
            assertTrue(deleteResult.isSuccess)

            assertTrue(awaitItem().isEmpty())
            assertNull(dao.getById("rec-del-online"))
            assertEquals(listOf("rec-del-online"), remoteDataSource.deletedEntryIds)
            assertEquals(0, syncScheduler.scheduleCallCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteEntry offline creates DELETED_PENDING tombstone excludes from UI queries and schedules sync`() = runTest {
        val fromEpoch = 1700000000000L
        val toEpoch = 1700086400000L
        val record = createRecord(id = "rec-del-offline", timestampMs = 1700005000000L)
        repository.addEntry(record)

        remoteDataSource.shouldFailDelete = DomainError.NetworkUnavailable("Offline")

        repository.observeDailyRecords(fromEpoch, toEpoch).test {
            assertEquals(1, awaitItem().size)

            val deleteResult = repository.deleteEntry(record.id)
            assertTrue(deleteResult.isSuccess)

            // Reactive Flow immediately hides the tombstoned record
            assertTrue(awaitItem().isEmpty())

            // Tombstone remains stored in Room with isDeleted = true and DELETED_PENDING status
            val stored = dao.getById("rec-del-offline")
            assertNotNull(stored)
            assertTrue(stored.isDeleted)
            assertEquals(SyncStatus.DELETED_PENDING.name, stored.syncStatus)

            // Excluded from daily records & pending additions, included in pending deletions
            assertTrue(repository.getDailyRecords(fromEpoch, toEpoch).getOrThrow().isEmpty())
            assertTrue(repository.getPendingRecords().isEmpty())
            assertEquals(listOf("rec-del-offline"), dao.getPendingDeletedRecords().map { it.id })
            assertEquals(1, syncScheduler.scheduleCallCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncPendingRecords syncs both PENDING additions and DELETED_PENDING tombstones`() = runTest {
        // 1. Add rec-1 while online so it exists on remote, then delete it while offline
        val toDelete = createRecord(id = "rec-to-delete", timestampMs = 1700005000000L)
        repository.addEntry(toDelete)

        remoteDataSource.shouldFailDelete = DomainError.NetworkUnavailable("Offline")
        repository.deleteEntry(toDelete.id)

        // 2. Add rec-2 while offline -> stays PENDING
        remoteDataSource.shouldFail = DomainError.NetworkUnavailable("Offline")
        val toAdd = createRecord(id = "rec-to-add", timestampMs = 1700006000000L)
        repository.addEntry(toAdd)

        assertEquals(1, dao.getPendingRecords().size)
        assertEquals(1, dao.getPendingDeletedRecords().size)

        // 3. Network returns
        remoteDataSource.shouldFail = null
        remoteDataSource.shouldFailDelete = null
        val syncResult = repository.syncPendingRecords()

        assertTrue(syncResult.isSuccess)
        assertEquals(2, syncResult.getOrThrow())

        // Addition is SYNCED, tombstone is permanently removed
        assertEquals(SyncStatus.SYNCED.name, dao.getById("rec-to-add")?.syncStatus)
        assertNull(dao.getById("rec-to-delete"))
        assertTrue(dao.getPendingRecords().isEmpty())
        assertTrue(dao.getPendingDeletedRecords().isEmpty())
        assertEquals(listOf("rec-to-delete"), remoteDataSource.deletedEntryIds)
        assertEquals(listOf("rec-to-add"), remoteDataSource.sentRecords.map { it.id.raw })
    }

    @Test
    fun `getRange hides DELETED_PENDING tombstones returned by remote and merges local PENDING entries`() = runTest {
        // Record on 2023-11-15 (epoch 1700006400_000 is 2023-11-15T00:00:00Z, 1700010000_000 is 01:00:00Z)
        val syncedRecord = createRecord(id = "rec-synced", timestampMs = 1700010000000L)
        val tombstonedRecord = createRecord(id = "rec-tombstone", timestampMs = 1700015000000L)
        repository.addEntry(syncedRecord)
        repository.addEntry(tombstonedRecord)

        // Tombstone rec-tombstone offline while remote still returns it
        remoteDataSource.shouldFailDelete = DomainError.NetworkUnavailable("Timeout")
        repository.deleteEntry(tombstonedRecord.id)

        // Add rec-pending offline
        remoteDataSource.shouldFail = DomainError.NetworkUnavailable("Timeout")
        val pendingRecord = createRecord(id = "rec-pending", timestampMs = 1700020000000L)
        repository.addEntry(pendingRecord)

        // Network recovers for getRange
        remoteDataSource.shouldFail = null
        val rangeResult = repository.getRange(
            fromIso = "2023-11-14",
            untilIso = "2023-11-16",
            targetUserId = null,
            timeZone = "UTC",
        )

        assertTrue(rangeResult.isSuccess)
        val ids = rangeResult.getOrThrow().map { it.id.raw }
        assertEquals(listOf("rec-synced", "rec-pending"), ids)
    }

    @Test
    fun `getRange with targetUserId queries remote for coach student and does not fall back to own Room`() = runTest {
        val ownRecord = createRecord(id = "rec-own", timestampMs = 1700010000000L)
        repository.addEntry(ownRecord)

        val studentId = UserId("student-42")
        val studentRecord = createRecord(id = "rec-student", timestampMs = 1700012000000L)
        remoteDataSource.recordsByTargetUser[studentId.raw] = listOf(studentRecord)

        val result = repository.getRange(
            fromIso = "2023-11-14",
            untilIso = "2023-11-16",
            targetUserId = studentId,
            timeZone = "UTC",
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(studentRecord), result.getOrThrow())
        assertEquals(studentId, remoteDataSource.lastGetRangeTargetUserId)

        // When student remote request fails, must NOT leak local coach Room data
        remoteDataSource.shouldFail = DomainError.Forbidden("No coach access")
        val errorResult = repository.getRange(
            fromIso = "2023-11-14",
            untilIso = "2023-11-16",
            targetUserId = studentId,
            timeZone = "UTC",
        )
        assertTrue(errorResult.isFailure)
        assertTrue(errorResult.exceptionOrNull() is DomainError.Forbidden)
    }

    private fun createRecord(
        id: String,
        key: IdempotencyKey = IdempotencyKey.generate(),
        timestampMs: Long = 1700000000000L,
    ) = MealRecord(
        id = MealRecordId(id),
        mealType = MealType.LUNCH,
        recordedAt = Instant.fromEpochMilliseconds(timestampMs),
        sourceType = EntrySourceType.PRODUCT,
        nameSnapshot = "Куриная грудка",
        weightGrams = 180.0,
        nutrition = NutritionFacts(calories = 220.0, proteins = 40.0, fats = 4.0, carbs = 2.0),
        cost = Money.fromRubles(95.0),
        idempotencyKey = key,
        syncStatus = SyncStatus.PENDING,
    )
}
