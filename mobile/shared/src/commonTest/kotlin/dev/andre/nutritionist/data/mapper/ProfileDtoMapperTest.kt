package dev.andre.nutritionist.data.mapper

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.TimestampProto
import dev.andre.nutritionist.data.mapper.ProfileDtoMapper.toDomain
import dev.andre.nutritionist.data.mapper.ProfileDtoMapper.toProto
import dev.andre.nutritionist.data.mapper.ProfileDtoMapper.toUpdateRequest
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileDtoMapperTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val proto = ProfileProto(
            id = "profile-100",
            userId = "user-200",
            heightCm = 178,
            weightKg = 74.5,
            gender = GenderProto.GENDER_MALE,
            activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_MODERATE,
            goalType = GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT,
            nutritionGoal = NutritionGoalProto(
                targetCalories = 2100,
                targetProteinsGrams = 150.0,
                targetFatsGrams = 60.0,
                targetCarbsGrams = 240.0,
                targetWeightKg = 70.0,
            ),
            updatedAt = TimestampProto(seconds = 1_700_000_000L, nanos = 500_000),
        )

        val domain = proto.toDomain()

        assertEquals(ProfileId("profile-100"), domain.id)
        assertEquals(UserId("user-200"), domain.userId)
        assertEquals(178, domain.heightCm)
        assertEquals(74.5, domain.weightKg)
        assertEquals(Gender.MALE, domain.gender)
        assertEquals(ActivityLevel.MODERATE, domain.activityLevel)
        assertEquals(GoalType.LOSE_WEIGHT, domain.goalType)
        assertEquals(2100, domain.nutritionGoal.targetCalories)
        assertEquals(150.0, domain.nutritionGoal.targetProteinsGrams)
        assertEquals(60.0, domain.nutritionGoal.targetFatsGrams)
        assertEquals(240.0, domain.nutritionGoal.targetCarbsGrams)
        assertEquals(70.0, domain.nutritionGoal.targetWeightKg)
        assertEquals(Instant.fromEpochSeconds(1_700_000_000L, 500_000), domain.updatedAt)
    }

    @Test
    fun `toDomain handles zero timestamp safely`() {
        val proto = ProfileProto(
            id = "p-1",
            userId = "u-1",
            updatedAt = TimestampProto(seconds = 0L, nanos = 0),
        )

        val domain = proto.toDomain()

        assertEquals(Instant.fromEpochMilliseconds(0L), domain.updatedAt)
    }

    @Test
    fun `toUpdateRequest maps domain to proto update request correctly`() {
        val domain = Profile(
            id = ProfileId("p-1"),
            userId = UserId("u-1"),
            heightCm = 185,
            weightKg = 82.0,
            gender = Gender.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            goalType = GoalType.MAINTAIN,
            nutritionGoal = NutritionGoal(
                targetCalories = 2400,
                targetProteinsGrams = 120.0,
                targetFatsGrams = 70.0,
                targetCarbsGrams = 320.0,
                targetWeightKg = 82.0,
            ),
            updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )

        val request = domain.toUpdateRequest()

        assertEquals("u-1", request.userId)
        assertEquals(185, request.heightCm)
        assertEquals(82.0, request.weightKg)
        assertEquals(GenderProto.GENDER_FEMALE, request.gender)
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_LIGHT, request.activityLevel)
        assertEquals(GoalTypeProto.GOAL_TYPE_MAINTAIN, request.goalType)
        assertEquals(2400, request.nutritionGoal.targetCalories)
        assertEquals(120.0, request.nutritionGoal.targetProteinsGrams)
        assertEquals(70.0, request.nutritionGoal.targetFatsGrams)
        assertEquals(320.0, request.nutritionGoal.targetCarbsGrams)
        assertEquals(82.0, request.nutritionGoal.targetWeightKg)
    }

    @Test
    fun `gender enum bidirectional mapping is exhaustive`() {
        assertEquals(Gender.MALE, GenderProto.GENDER_MALE.toDomain())
        assertEquals(Gender.FEMALE, GenderProto.GENDER_FEMALE.toDomain())
        assertEquals(Gender.UNKNOWN, GenderProto.GENDER_UNSPECIFIED.toDomain())
        assertEquals(Gender.UNKNOWN, GenderProto.UNRECOGNIZED.toDomain())

        assertEquals(GenderProto.GENDER_MALE, Gender.MALE.toProto())
        assertEquals(GenderProto.GENDER_FEMALE, Gender.FEMALE.toProto())
        assertEquals(GenderProto.GENDER_UNSPECIFIED, Gender.UNKNOWN.toProto())
    }

    @Test
    fun `activity level enum bidirectional mapping is exhaustive`() {
        assertEquals(ActivityLevel.SEDENTARY, ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY.toDomain())
        assertEquals(ActivityLevel.LIGHT, ActivityLevelProto.ACTIVITY_LEVEL_LIGHT.toDomain())
        assertEquals(ActivityLevel.MODERATE, ActivityLevelProto.ACTIVITY_LEVEL_MODERATE.toDomain())
        assertEquals(ActivityLevel.VERY_ACTIVE, ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE.toDomain())
        assertEquals(ActivityLevel.UNKNOWN, ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED.toDomain())
        assertEquals(ActivityLevel.UNKNOWN, ActivityLevelProto.UNRECOGNIZED.toDomain())

        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_SEDENTARY, ActivityLevel.SEDENTARY.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_LIGHT, ActivityLevel.LIGHT.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE, ActivityLevel.MODERATE.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_VERY_ACTIVE, ActivityLevel.VERY_ACTIVE.toProto())
        assertEquals(ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED, ActivityLevel.UNKNOWN.toProto())
    }

    @Test
    fun `goal type enum bidirectional mapping is exhaustive`() {
        assertEquals(GoalType.LOSE_WEIGHT, GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT.toDomain())
        assertEquals(GoalType.MAINTAIN, GoalTypeProto.GOAL_TYPE_MAINTAIN.toDomain())
        assertEquals(GoalType.GAIN_WEIGHT, GoalTypeProto.GOAL_TYPE_GAIN_WEIGHT.toDomain())
        assertEquals(GoalType.UNKNOWN, GoalTypeProto.GOAL_TYPE_UNSPECIFIED.toDomain())
        assertEquals(GoalType.UNKNOWN, GoalTypeProto.UNRECOGNIZED.toDomain())

        assertEquals(GoalTypeProto.GOAL_TYPE_LOSE_WEIGHT, GoalType.LOSE_WEIGHT.toProto())
        assertEquals(GoalTypeProto.GOAL_TYPE_MAINTAIN, GoalType.MAINTAIN.toProto())
        assertEquals(GoalTypeProto.GOAL_TYPE_GAIN_WEIGHT, GoalType.GAIN_WEIGHT.toProto())
        assertEquals(GoalTypeProto.GOAL_TYPE_UNSPECIFIED, GoalType.UNKNOWN.toProto())
    }
}
