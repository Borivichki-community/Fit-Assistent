package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.TimestampProto
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeProfileGrpcServiceClient
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileGrpcRemoteDataSourceTest {

    private val fakeClient = FakeProfileGrpcServiceClient()
    private val dispatchers = TestCoroutineDispatchers()
    private val dataSource = ProfileGrpcRemoteDataSource(fakeClient, dispatchers)

    @BeforeTest
    fun setUp() {
        fakeClient.reset()
    }

    @Test
    fun `getProfile returns success and maps domain model correctly`() = runTest {
        val testProto = ProfileProto(
            id = "profile-1",
            userId = "user-1",
            heightCm = 175,
            weightKg = 70.0,
            gender = GenderProto.GENDER_FEMALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_MODERATE,
            goalType = GoalTypeProto.GOAL_TYPE_MAINTAIN,
            nutritionGoal = NutritionGoalProto(
                targetCalories = 2000,
                targetProteinsGrams = 120.0,
                targetFatsGrams = 60.0,
                targetCarbsGrams = 245.0,
                targetWeightKg = 70.0,
            ),
            updatedAt = TimestampProto(seconds = 1_700_000_000L, nanos = 0),
        )
        fakeClient.responseToReturn = testProto

        val result = dataSource.getProfile(UserId("user-1"))

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(ProfileId("profile-1"), profile.id)
        assertEquals(UserId("user-1"), profile.userId)
        assertEquals(175, profile.heightCm)
        assertEquals(70.0, profile.weightKg)
        assertEquals(Gender.FEMALE, profile.gender)
        assertEquals(ActivityLevel.MODERATE, profile.activityLevel)
        assertEquals(GoalType.MAINTAIN, profile.goalType)
        assertEquals(2000, profile.nutritionGoal.targetCalories)
        assertEquals(120.0, profile.nutritionGoal.targetProteinsGrams)
        assertEquals(60.0, profile.nutritionGoal.targetFatsGrams)
        assertEquals(245.0, profile.nutritionGoal.targetCarbsGrams)
        assertEquals(70.0, profile.nutritionGoal.targetWeightKg)
        assertEquals(Instant.fromEpochSeconds(1_700_000_000L, 0), profile.updatedAt)

        assertEquals("user-1", fakeClient.lastGetRequest?.userId)
    }

    @Test
    fun `getProfile returns failure when client throws exception`() = runTest {
        val networkException = IllegalStateException("gRPC UNAVAILABLE: connection refused")
        fakeClient.shouldThrow = networkException

        val result = dataSource.getProfile(UserId("user-1"))

        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals("gRPC UNAVAILABLE: connection refused", result.exceptionOrNull()?.message)
    }

    @Test
    fun `updateProfile sends mapped request and returns success domain model`() = runTest {
        val inputProfile = Profile(
            id = ProfileId("p-99"),
            userId = UserId("u-99"),
            heightCm = 190,
            weightKg = 95.0,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.VERY_ACTIVE,
            goalType = GoalType.LOSE_WEIGHT,
            nutritionGoal = NutritionGoal(
                targetCalories = 2500,
                targetProteinsGrams = 200.0,
                targetFatsGrams = 80.0,
                targetCarbsGrams = 245.0,
                targetWeightKg = 85.0,
            ),
            updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )

        val updatedResponseProto = ProfileProto(
            id = "p-99",
            userId = "u-99",
            heightCm = 190,
            weightKg = 95.0,
            gender = GenderProto.GENDER_MALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE,
            goalType = GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT,
            nutritionGoal = NutritionGoalProto(
                targetCalories = 2500,
                targetProteinsGrams = 200.0,
                targetFatsGrams = 80.0,
                targetCarbsGrams = 245.0,
                targetWeightKg = 85.0,
            ),
            updatedAt = TimestampProto(seconds = 1_700_000_005L, nanos = 0),
        )
        fakeClient.responseToReturn = updatedResponseProto

        val result = dataSource.updateProfile(inputProfile)

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals(ProfileId("p-99"), saved.id)
        assertEquals(UserId("u-99"), saved.userId)
        assertEquals(190, saved.heightCm)
        assertEquals(95.0, saved.weightKg)
        assertEquals(Gender.MALE, saved.gender)
        assertEquals(ActivityLevel.VERY_ACTIVE, saved.activityLevel)
        assertEquals(GoalType.LOSE_WEIGHT, saved.goalType)
        assertEquals(2500, saved.nutritionGoal.targetCalories)

        // Проверяем, что запрос ушёл с правильными полями
        val sentRequest = fakeClient.lastUpdateRequest
        assertEquals("u-99", sentRequest?.userId)
        assertEquals(190, sentRequest?.heightCm)
        assertEquals(95.0, sentRequest?.weightKg)
        assertEquals(GenderProto.GENDER_MALE, sentRequest?.gender)
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE, sentRequest?.activityLevel)
        assertEquals(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT, sentRequest?.goalType)
        assertEquals(2500, sentRequest?.nutritionGoal?.targetCalories)
    }

    @Test
    fun `updateProfile returns failure when client throws exception`() = runTest {
        val inputProfile = Profile(
            id = ProfileId("p-1"),
            userId = UserId("u-1"),
            heightCm = 170,
            weightKg = 65.0,
            gender = Gender.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            goalType = GoalType.MAINTAIN,
            nutritionGoal = NutritionGoal(1800, 100.0, 50.0, 237.5, 65.0),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        fakeClient.shouldThrow = RuntimeException("gRPC DEADLINE_EXCEEDED")

        val result = dataSource.updateProfile(inputProfile)

        assertTrue(result.isFailure)
        assertIs<RuntimeException>(result.exceptionOrNull())
        assertEquals("gRPC DEADLINE_EXCEEDED", result.exceptionOrNull()?.message)
    }
}
