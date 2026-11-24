package dev.andre.nutritionist.data.local

import androidx.room.Room
import app.cash.turbine.test
import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MealRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MealRecordDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
            .allowMainThreadQueries()
            .build()
        dao = database.mealRecordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById_success() = runTest {
        val entity = createEntity(id = "rec-1", name = "Куриное филе")
        dao.insert(entity)

        val loaded = dao.getById("rec-1")
        assertNotNull(loaded)
        assertEquals("rec-1", loaded?.id)
        assertEquals("Куриное филе", loaded?.nameSnapshot)
        assertEquals("PENDING", loaded?.syncStatus)
        assertEquals("idem-key-1", loaded?.idempotencyKey)
    }

    @Test
    fun getPendingRecords_returnsOnlyPending() = runTest {
        val pending1 = createEntity(id = "p-1", syncStatus = "PENDING")
        val synced = createEntity(id = "s-1", syncStatus = "SYNCED")
        val pending2 = createEntity(id = "p-2", syncStatus = "PENDING")

        dao.insert(pending1)
        dao.insert(synced)
        dao.insert(pending2)

        val pendingList = dao.getPendingRecords()
        assertEquals(2, pendingList.size)
        assertTrue(pendingList.any { it.id == "p-1" })
        assertTrue(pendingList.any { it.id == "p-2" })
    }

    @Test
    fun updateSyncStatus_updatesStatusCorrectly() = runTest {
        val entity = createEntity(id = "rec-sync", syncStatus = "PENDING")
        dao.insert(entity)

        dao.updateSyncStatus("rec-sync", "SYNCED")

        val updated = dao.getById("rec-sync")
        assertNotNull(updated)
        assertEquals("SYNCED", updated?.syncStatus)
    }

    @Test
    fun getDailyRecordsFlow_filtersDateRangeAndEmitsTurbine() = runTest {
        val dayStart = 1700000000000L
        val dayEnd = 1700086400000L

        val inRange1 = createEntity(id = "r-1", timestampEpoch = 1700010000000L)
        val inRange2 = createEntity(id = "r-2", timestampEpoch = 1700050000000L)
        val outOfRange = createEntity(id = "r-out", timestampEpoch = 1700100000000L)

        dao.getDailyRecordsFlow(dayStart, dayEnd).test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            dao.insert(inRange1)
            val first = awaitItem()
            assertEquals(1, first.size)
            assertEquals("r-1", first.first().id)

            dao.insert(inRange2)
            val second = awaitItem()
            assertEquals(2, second.size)

            dao.insert(outOfRange) // Out of range, should not change filtered list
            // No emission expected for out-of-range record query, or same items
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteById_removesRecord() = runTest {
        val entity = createEntity(id = "to-del")
        dao.insert(entity)
        assertNotNull(dao.getById("to-del"))

        dao.deleteById("to-del")
        assertNull(dao.getById("to-del"))
    }

    private fun createEntity(
        id: String,
        name: String = "Test Meal",
        syncStatus: String = "PENDING",
        timestampEpoch: Long = 1700000000000L,
    ) = MealRecordEntity(
        id = id,
        idempotencyKey = "idem-key-1",
        syncStatus = syncStatus,
        mealType = "BREAKFAST",
        recordedAtEpoch = timestampEpoch,
        sourceType = "PRODUCT",
        sourceId = "prod-1",
        nameSnapshot = name,
        weightGrams = 200.0,
        calories = 250.0,
        proteins = 20.0,
        fats = 5.0,
        carbs = 30.0,
        costAmountMinor = 5000L,
        costCurrency = "RUB",
    )
}
