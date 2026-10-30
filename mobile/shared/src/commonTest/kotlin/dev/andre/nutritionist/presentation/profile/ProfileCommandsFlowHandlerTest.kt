package dev.andre.nutritionist.presentation.profile

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeRepository: FakeProfileRepository
    private lateinit var handler: ProfileCommandsFlowHandler

    private val testUserId = UserId("user-test-1")
    private val testProfile = Profile(
        id = ProfileId("prof-1"),
        userId = testUserId,
        heightCm = 180,
        weightKg = 75.0,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.LOSE_WEIGHT,
        nutritionGoal = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 150.0,
            targetFatsGrams = 60.0,
            targetCarbsGrams = 200.0,
            targetWeightKg = 70.0,
        ),
        weeklyBudget = 5000.0,
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @BeforeTest
    fun setUp() {
        fakeRepository = FakeProfileRepository()
        handler = ProfileCommandsFlowHandler(fakeRepository, testDispatchers)
    }

    @Test
    fun `LoadProfile command emits ProfileLoaded event on success`() = runTest(testDispatcher) {
        fakeRepository.profileToReturn = testProfile

        handler.handle(flowOf(ProfileCommand.LoadProfile(testUserId))).test {
            val event = awaitItem()
            assertIs<ProfileEvent.Internal.ProfileLoaded>(event)
            assertEquals(testProfile, event.profile)
            awaitComplete()
        }
    }

    @Test
    fun `LoadProfile command emits ProfileLoadFailed event on failure`() = runTest(testDispatcher) {
        val error = RuntimeException("Сетевой сбой")
        fakeRepository.errorToThrow = error

        handler.handle(flowOf(ProfileCommand.LoadProfile(testUserId))).test {
            val event = awaitItem()
            assertIs<ProfileEvent.Internal.ProfileLoadFailed>(event)
            assertEquals("Сетевой сбой", event.error.message)
            awaitComplete()
        }
    }

    @Test
    fun `LoadProfile command rethrows CancellationException`() = runTest(testDispatcher) {
        fakeRepository.errorToThrow = kotlinx.coroutines.CancellationException("Test cancellation")

        handler.handle(flowOf(ProfileCommand.LoadProfile(testUserId))).test {
            val error = awaitError()
            assertIs<kotlinx.coroutines.CancellationException>(error)
        }
    }

    private class FakeProfileRepository : ProfileRepository {
        var profileToReturn: Profile? = null
        var errorToThrow: Throwable? = null

        override suspend fun getProfile(userId: UserId): Profile {
            errorToThrow?.let { throw it }
            return profileToReturn ?: error("Профиль не задан в FakeProfileRepository")
        }

        override suspend fun updateProfile(profile: Profile): Profile {
            errorToThrow?.let { throw it }
            profileToReturn = profile
            return profile
        }
    }
}
