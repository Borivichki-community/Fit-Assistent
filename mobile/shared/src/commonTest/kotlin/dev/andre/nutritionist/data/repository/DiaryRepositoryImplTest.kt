package dev.andre.nutritionist.data.repository

import app.cash.turbine.test
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.fake.FakeDiaryRemoteDataSource
import dev.andre.nutritionist.fake.FakeMealRecordDao
import dev.andre.nutritionist.fake.FakeSyncScheduler
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DiaryRepositoryImplTest {

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
    fun `addEntry happy path saves to Room as PENDING then updates to SYNCED on remote success`() = runTest {
        val key = IdempotencyKey.generate()
        val record = createRecord(id = "rec-1", key = key)

        val result = repository.addEntry(record)

        assertTrue(result.isSuccess)
        val returned = result.getOrThrow()
        assertEquals(SyncStatus.SYNCED, returned.syncStatus)

        // Verify Room has SYNCED and SyncScheduler was not triggered
        val inRoom = dao.getById("rec-1")
        assertNotNull(inRoom)
        assertEquals("SYNCED", inRoom.syncStatus)
        assertEquals(key.raw, inRoom.idempotencyKey)
        assertEquals(0, syncScheduler.scheduleCallCount)

        // Verify remote was called once with that idempotency key
        assertEquals(1, remoteDataSource.addEntryCallCount)
        assertEquals(listOf(key.raw), remoteDataSource.receivedIdempotencyKeys)
    }

    @Test
    fun `addEntry offline-first saves PENDING preserves idempotencyKey and schedules background sync`() = runTest {
        remoteDataSource.shouldFail = RuntimeException("Network timeout")
        val key = IdempotencyKey.generate()
        val record = createRecord(id = "rec-offline", key = key)

        val result = repository.addEntry(record)

        // Even though remote failed, local save succeeded (offline-first)
        assertTrue(result.isSuccess)
        val returned = result.getOrThrow()
        assertEquals(SyncStatus.PENDING, returned.syncStatus)
        assertEquals(1, syncScheduler.scheduleCallCount)

        // Room keeps PENDING with unchanged idempotencyKey
        val inRoom = dao.getById("rec-offline")
        assertNotNull(inRoom)
        assertEquals("PENDING", inRoom.syncStatus)
        assertEquals(key.raw, inRoom.idempotencyKey)
    }

    @Test
    fun `syncPendingRecords resends PENDING records with SAME idempotencyKey and marks SYNCED`() = runTest {
        // Prepare offline record in Room
        val key = IdempotencyKey.generate()
        remoteDataSource.shouldFail = RuntimeException("Network offline")
        repository.addEntry(createRecord(id = "rec-pending", key = key))

        assertEquals(1, dao.getPendingRecords().size)

        // Network is restored!
        remoteDataSource.shouldFail = null
        val syncResult = repository.syncPendingRecords()

        assertTrue(syncResult.isSuccess)
        assertEquals(1, syncResult.getOrThrow())

        // Verify record in Room is now SYNCED
        val inRoom = dao.getById("rec-pending")
        assertNotNull(inRoom)
        assertEquals("SYNCED", inRoom.syncStatus)

        // Verify the SAME idempotency key was used on retry
        assertEquals(2, remoteDataSource.receivedIdempotencyKeys.size)
        assertEquals(key.raw, remoteDataSource.receivedIdempotencyKeys[0])
        assertEquals(key.raw, remoteDataSource.receivedIdempotencyKeys[1])
    }

    @Test
    fun `observeDailyRecords emits Room changes reactively`() = runTest {
        val fromEpoch = 1700000000000L
        val toEpoch = 1700086400000L

        repository.observeDailyRecords(fromEpoch, toEpoch).test {
            // Initial empty
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            // Add an entry within date range
            val record = createRecord(id = "rec-in-range", timestampMs = 1700005000000L)
            repository.addEntry(record)

            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("rec-in-range", updated.first().id.raw)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createRecord(
        id: String,
        key: IdempotencyKey = IdempotencyKey.generate(),
        timestampMs: Long = 1700000000000L,
    ) = MealRecord(
        id = MealRecordId(id),
        mealType = MealType.BREAKFAST,
        recordedAt = Instant.fromEpochMilliseconds(timestampMs),
        sourceType = EntrySourceType.PRODUCT,
        nameSnapshot = "Овсянка",
        weightGrams = 200.0,
        nutrition = NutritionFacts(calories = 200.0, proteins = 8.0, fats = 4.0, carbs = 34.0),
        cost = Money.fromRubles(40.0),
        idempotencyKey = key,
        syncStatus = SyncStatus.PENDING,
    )
}
