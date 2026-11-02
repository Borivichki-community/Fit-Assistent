package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.ProfileEntity
import dev.andre.nutritionist.data.mapper.ProfileEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.ProfileEntityMapper.toEntity
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

class ProfileEntityMapperTest {

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val entity = ProfileEntity(
            userId = "usr-1",
            profileId = "prf-1",
            heightCm = 175,
            weightKg = 70.0,
            gender = "FEMALE",
            activityLevel = "LIGHT",
            goalType = "LOSE_WEIGHT",
            targetCalories = 2100,
            targetProteinsGrams = 130.0,
            targetFatsGrams = 60.0,
            targetCarbsGrams = 240.0,
            targetWeightKg = 65.0,
            weeklyBudget = 4500.0,
            updatedAtEpochMs = 1730000000000L,
        )

        val domain = entity.toDomain()

        assertEquals(expected = UserId("usr-1"), actual = domain.userId)
        assertEquals(expected = ProfileId("prf-1"), actual = domain.id)
        assertEquals(expected = 175, actual = domain.heightCm)
        assertEquals(expected = 70.0, actual = domain.weightKg)
        assertEquals(expected = Gender.FEMALE, actual = domain.gender)
        assertEquals(expected = ActivityLevel.LIGHT, actual = domain.activityLevel)
        assertEquals(expected = GoalType.LOSE_WEIGHT, actual = domain.goalType)
        assertEquals(expected = 2100, actual = domain.nutritionGoal.targetCalories)
        assertEquals(expected = 130.0, actual = domain.nutritionGoal.targetProteinsGrams)
        assertEquals(expected = 60.0, actual = domain.nutritionGoal.targetFatsGrams)
        assertEquals(expected = 240.0, actual = domain.nutritionGoal.targetCarbsGrams)
        assertEquals(expected = 65.0, actual = domain.nutritionGoal.targetWeightKg)
        assertEquals(expected = 4500.0, actual = domain.weeklyBudget)
        assertEquals(expected = Instant.fromEpochMilliseconds(1730000000000L), actual = domain.updatedAt)
    }

    @Test
    fun toEntity_mapsAllFieldsCorrectly() {
        val domain = Profile(
            id = ProfileId("prf-2"),
            userId = UserId("usr-2"),
            heightCm = 185,
            weightKg = 85.0,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.VERY_ACTIVE,
            goalType = GoalType.GAIN_WEIGHT,
            nutritionGoal = NutritionGoal(
                targetCalories = 3200,
                targetProteinsGrams = 180.0,
                targetFatsGrams = 90.0,
                targetCarbsGrams = 400.0,
                targetWeightKg = 90.0,
            ),
            weeklyBudget = 7500.0,
            updatedAt = Instant.fromEpochMilliseconds(1730005000000L),
        )

        val entity = domain.toEntity()

        assertEquals(expected = "usr-2", actual = entity.userId)
        assertEquals(expected = "prf-2", actual = entity.profileId)
        assertEquals(expected = 185, actual = entity.heightCm)
        assertEquals(expected = 85.0, actual = entity.weightKg)
        assertEquals(expected = "MALE", actual = entity.gender)
        assertEquals(expected = "VERY_ACTIVE", actual = entity.activityLevel)
        assertEquals(expected = "GAIN_WEIGHT", actual = entity.goalType)
        assertEquals(expected = 3200, actual = entity.targetCalories)
        assertEquals(expected = 180.0, actual = entity.targetProteinsGrams)
        assertEquals(expected = 90.0, actual = entity.targetFatsGrams)
        assertEquals(expected = 400.0, actual = entity.targetCarbsGrams)
        assertEquals(expected = 90.0, actual = entity.targetWeightKg)
        assertEquals(expected = 7500.0, actual = entity.weeklyBudget)
        assertEquals(expected = 1730005000000L, actual = entity.updatedAtEpochMs)
    }

    @Test
    fun toDomain_fallbackToUnknown_onInvalidEnums() {
        val entity = ProfileEntity(
            userId = "usr-3",
            profileId = "prf-3",
            heightCm = 170,
            weightKg = 65.0,
            gender = "INVALID_GENDER",
            activityLevel = "UNKNOWN_LEVEL",
            goalType = "SOMETHING_ELSE",
            targetCalories = 2000,
            targetProteinsGrams = 100.0,
            targetFatsGrams = 50.0,
            targetCarbsGrams = 200.0,
            targetWeightKg = 65.0,
            weeklyBudget = 3000.0,
            updatedAtEpochMs = 0L,
        )

        val domain = entity.toDomain()

        assertEquals(expected = Gender.UNKNOWN, actual = domain.gender)
        assertEquals(expected = ActivityLevel.UNKNOWN, actual = domain.activityLevel)
        assertEquals(expected = GoalType.UNKNOWN, actual = domain.goalType)
    }
}
