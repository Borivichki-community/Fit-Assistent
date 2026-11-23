package dev.andre.nutritionist.data.local

import androidx.room.Room
import app.cash.turbine.test
import dev.andre.nutritionist.data.local.dao.DiaryDao
import dev.andre.nutritionist.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DiaryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DiaryDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
            .build()
        dao = database.diaryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetDailyEntries_success() = runTest {
        val entry1 = createEntity(id = "e-1", userId = "user-1", dateEpochDay = 100L, calories = 300)
        val entry2 = createEntity(id = "e-2", userId = "user-1", dateEpochDay = 100L, calories = 500)
        val entryOtherDay = createEntity(id = "e-3", userId = "user-1", dateEpochDay = 101L, calories = 400)

        dao.insertOrReplace(entry1)
        dao.insertOrReplace(entry2)
        dao.insertOrReplace(entryOtherDay)

        val daily = dao.getDailyEntries("user-1", 100L)
        assertEquals(2, daily.size)
        assertEquals(listOf("e-1", "e-2"), daily.map { it.id })
    }

    @Test
    fun getPendingEntries_and_updateSyncStatus() = runTest {
        val pendingEntry = createEntity(id = "e-pending", syncStatus = "SYNC_PENDING")
        val syncedEntry = createEntity(id = "e-synced", syncStatus = "SYNCED")

        dao.insertOrReplace(pendingEntry)
        dao.insertOrReplace(syncedEntry)

        val pendingList = dao.getPendingEntries()
        assertEquals(1, pendingList.size)
        assertEquals("e-pending", pendingList.first().id)

        dao.updateSyncStatus("e-pending", "SYNCED")
        val emptyPending = dao.getPendingEntries()
        assertTrue(emptyPending.isEmpty())
    }

    @Test
    fun observeDailyEntries_emitsUpdates() = runTest {
        val entry = createEntity(id = "e-obs", userId = "user-obs", dateEpochDay = 200L)

        dao.observeDailyEntries("user-obs", 200L).test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            dao.insertOrReplace(entry)
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("e-obs", updated.first().id)

            dao.deleteById("e-obs")
            val empty = awaitItem()
            assertTrue(empty.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createEntity(
        id: String,
        userId: String = "user-default",
        foodItemId: String = "food-1",
        foodName: String = "Курица гриль",
        portionGrams: Double = 150.0,
        calories: Int = 250,
        proteins: Double = 30.0,
        fats: Double = 5.0,
        carbs: Double = 0.0,
        mealType: String = "LUNCH",
        dateEpochDay: Long = 100L,
        recordedAtEpochMs: Long = 1000L,
        idempotencyKey: String = "idem-default",
        syncStatus: String = "SYNC_PENDING",
    ) = DiaryEntryEntity(
        id = id,
        userId = userId,
        foodItemId = foodItemId,
        foodName = foodName,
        portionGrams = portionGrams,
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbs = carbs,
        mealType = mealType,
        dateEpochDay = dateEpochDay,
        recordedAtEpochMs = recordedAtEpochMs,
        idempotencyKey = idempotencyKey,
        portionDescription = "150 г, порция",
        syncStatus = syncStatus,
    )
}
