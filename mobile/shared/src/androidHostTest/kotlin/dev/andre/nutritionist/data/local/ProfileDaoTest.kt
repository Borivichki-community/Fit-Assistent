package dev.andre.nutritionist.data.local

import androidx.room.Room
import app.cash.turbine.test
import dev.andre.nutritionist.data.local.dao.ProfileDao
import dev.andre.nutritionist.data.local.entity.ProfileEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ProfileDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
            .build()
        dao = database.profileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetProfile_success() = runTest {
        val entity = createEntity(userId = "user_42", profileId = "profile_100", weightKg = 75.5)

        dao.insertOrReplace(entity)
        val loaded = dao.getProfile(userId = "user_42")

        assertNotNull(loaded)
        assertEquals(expected = "user_42", actual = loaded.userId)
        assertEquals(expected = "profile_100", actual = loaded.profileId)
        assertEquals(expected = 180, actual = loaded.heightCm)
        assertEquals(expected = 75.5, actual = loaded.weightKg)
        assertEquals(expected = 2500, actual = loaded.targetCalories)
    }

    @Test
    fun getProfile_returnsNull_whenNotFound() = runTest {
        val loaded = dao.getProfile(userId = "non_existent")
        assertNull(loaded)
    }

    @Test
    fun insertOrReplace_updatesExistingProfile() = runTest {
        val initial = createEntity(userId = "user_42", weightKg = 80.0, weeklyBudget = 5000.0)
        dao.insertOrReplace(initial)

        val updated = createEntity(userId = "user_42", weightKg = 77.5, weeklyBudget = 6000.0)
        dao.insertOrReplace(updated)

        val loaded = dao.getProfile(userId = "user_42")
        assertNotNull(loaded)
        assertEquals(expected = 77.5, actual = loaded.weightKg)
        assertEquals(expected = 6000.0, actual = loaded.weeklyBudget)
    }

    @Test
    fun observeProfile_emitsUpdatesTurbine() = runTest {
        val initial = createEntity(userId = "user_observe", weightKg = 70.0)

        dao.observeProfile("user_observe").test {
            assertNull(awaitItem())

            dao.insertOrReplace(initial)
            val firstEmission = awaitItem()
            assertNotNull(firstEmission)
            assertEquals(expected = 70.0, actual = firstEmission.weightKg)

            val updated = initial.copy(weightKg = 68.5)
            dao.insertOrReplace(updated)
            val secondEmission = awaitItem()
            assertNotNull(secondEmission)
            assertEquals(expected = 68.5, actual = secondEmission.weightKg)

            dao.clear("user_observe")
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createEntity(
        userId: String,
        profileId: String = "profile_default",
        weightKg: Double = 75.0,
        weeklyBudget: Double = 5000.0,
    ) = ProfileEntity(
        userId = userId,
        profileId = profileId,
        heightCm = 180,
        weightKg = weightKg,
        gender = "MALE",
        activityLevel = "MODERATE",
        goalType = "MAINTAIN",
        targetCalories = 2500,
        targetProteinsGrams = 150.0,
        targetFatsGrams = 70.0,
        targetCarbsGrams = 300.0,
        targetWeightKg = 75.0,
        weeklyBudget = weeklyBudget,
        updatedAtEpochMs = 1730000000000L,
    )
}
