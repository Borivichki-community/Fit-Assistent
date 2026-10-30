package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileRepositoryImplTest {

    private val testUserId = UserId("user-123")
    private val testProfile = Profile(
        id = ProfileId("prof-1"),
        userId = testUserId,
        heightCm = 175,
        weightKg = 70.0,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN,
        nutritionGoal = NutritionGoal(
            targetCalories = 2200,
            targetProteinsGrams = 140.0,
            targetFatsGrams = 70.0,
            targetCarbsGrams = 250.0,
            targetWeightKg = 70.0,
        ),
        weeklyBudget = 4500.0,
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    private val fakeRemoteDataSource = FakeProfileRemoteDataSource()
    private val repository = ProfileRepositoryImpl(remoteDataSource = fakeRemoteDataSource)

    @Test
    fun `getProfile success returns domain profile`() = runTest {
        fakeRemoteDataSource.profileToReturn = testProfile

        val result = repository.getProfile(testUserId)

        assertEquals(testProfile, result)
        assertEquals(testUserId, fakeRemoteDataSource.lastRequestedUserId)
    }

    @Test
    fun `getProfile failure throws exception`() = runTest {
        fakeRemoteDataSource.errorToThrow = RuntimeException("Сетевой сбой")

        assertFailsWith<RuntimeException> {
            repository.getProfile(testUserId)
        }
    }

    @Test
    fun `updateProfile success returns updated domain profile`() = runTest {
        val updatedProfile = testProfile.copy(heightCm = 176)
        fakeRemoteDataSource.profileToReturn = updatedProfile

        val result = repository.updateProfile(updatedProfile)

        assertEquals(updatedProfile, result)
        assertEquals(updatedProfile, fakeRemoteDataSource.lastUpdatedProfile)
    }

    @Test
    fun `updateProfile failure throws exception`() = runTest {
        fakeRemoteDataSource.errorToThrow = IllegalStateException("Ошибка обновления")

        assertFailsWith<IllegalStateException> {
            repository.updateProfile(testProfile)
        }
    }

    private class FakeProfileRemoteDataSource : ProfileRemoteDataSource {
        var profileToReturn: Profile? = null
        var errorToThrow: Throwable? = null
        var lastRequestedUserId: UserId? = null
        var lastUpdatedProfile: Profile? = null

        override suspend fun getProfile(userId: UserId): Result<Profile> {
            lastRequestedUserId = userId
            errorToThrow?.let { return Result.failure(it) }
            val profile = profileToReturn ?: return Result.failure(NoSuchElementException("No profile"))
            return Result.success(profile)
        }

        override suspend fun updateProfile(profile: Profile): Result<Profile> {
            lastUpdatedProfile = profile
            errorToThrow?.let { return Result.failure(it) }
            val returnProfile = profileToReturn ?: profile
            return Result.success(returnProfile)
        }
    }
}
