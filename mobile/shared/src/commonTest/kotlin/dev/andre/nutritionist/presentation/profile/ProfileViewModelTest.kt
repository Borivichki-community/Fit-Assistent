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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = dev.andre.nutritionist.fake.TestCoroutineDispatchers(testDispatcher)

    private lateinit var fakeRepository: FakeProfileRepository
    private lateinit var viewModel: ProfileViewModel
    private lateinit var update: ProfileUpdate
    private lateinit var commandsFlowHandler: ProfileCommandsFlowHandler
    private lateinit var uiStateMapper: ProfileUiStateMapper

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
        update = ProfileUpdate()
        commandsFlowHandler = ProfileCommandsFlowHandler(fakeRepository, testDispatchers)
        uiStateMapper = ProfileUiStateMapper()

        viewModel = ProfileViewModel(
            update = update,
            commandsFlowHandler = commandsFlowHandler,
            uiStateMapper = uiStateMapper
        )
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        viewModel.state.test {
            val initialState = awaitItem()
            assertIs<ProfileUiState.Content.Loading>(initialState.content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LoadProfile event successfully updates state to Success`() = runTest(testDispatcher) {
        fakeRepository.profileToReturn = testProfile

        viewModel.state.test {
            // Initial state
            assertIs<ProfileUiState.Content.Loading>(awaitItem().content)

            // Dispatch load
            viewModel.dispatch(ProfileEvent.Ui.LoadProfile(testUserId))

            // Then transitions to success
            val successState = awaitItem()
            assertIs<ProfileUiState.Content.Success>(successState.content)
            val uiModel = (successState.content as ProfileUiState.Content.Success).profile
            assertEquals(testProfile.id, uiModel.id)
            assertEquals("Мужской", uiModel.genderLabel)
        }
    }

    @Test
    fun `LoadProfile event failure updates state to Error and emits news`() = runTest(testDispatcher) {
        fakeRepository.errorToThrow = RuntimeException("Test Error Message")

        viewModel.state.test {
            // Initial state
            assertIs<ProfileUiState.Content.Loading>(awaitItem().content)

            viewModel.news.test {
                // Dispatch load
                viewModel.dispatch(ProfileEvent.Ui.LoadProfile(testUserId))

                // Verify news emitted
                val newsItem = awaitItem()
                assertIs<ProfileNews.ShowSnackbar>(newsItem)
                assertEquals("Test Error Message", newsItem.message)

                cancelAndIgnoreRemainingEvents()
            }

            // Verify state transition to error
            val errorState = awaitItem()
            assertIs<ProfileUiState.Content.Error>(errorState.content)
            assertEquals("Test Error Message", (errorState.content as ProfileUiState.Content.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeProfileRepository : ProfileRepository {
        var profileToReturn: Profile? = null
        var errorToThrow: Throwable? = null

        override fun observeProfile(userId: UserId): Flow<Profile?> = flow {
            emit(profileToReturn)
        }

        override suspend fun getProfile(userId: UserId): Profile? {
            return profileToReturn
        }

        override suspend fun refreshProfile(userId: UserId): Result<Unit> {
            val error = errorToThrow
            if (error != null) {
                return Result.failure(error)
            }
            return Result.success(Unit)
        }

        override suspend fun updateProfile(profile: Profile): Result<Profile> {
            val error = errorToThrow
            if (error != null) {
                return Result.failure(error)
            }
            profileToReturn = profile
            return Result.success(profile)
        }
    }
}
