package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.ProfileEntity
import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.ProfileId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.Instant

object ProfileEntityMapper {

    fun ProfileEntity.toDomain(): Profile {
        val nutritionGoal = NutritionGoal(
            targetCalories = targetCalories,
            targetProteinsGrams = targetProteinsGrams,
            targetFatsGrams = targetFatsGrams,
            targetCarbsGrams = targetCarbsGrams,
            targetWeightKg = targetWeightKg,
        )

        return Profile(
            id = ProfileId(profileId),
            userId = UserId(userId),
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender.toGender(),
            activityLevel = activityLevel.toActivityLevel(),
            goalType = goalType.toGoalType(),
            nutritionGoal = nutritionGoal,
            weeklyBudget = weeklyBudget,
            updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMs),
        )
    }

    fun Profile.toEntity(): ProfileEntity {
        return ProfileEntity(
            userId = userId.raw,
            profileId = id.raw,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = when (gender) {
                Gender.MALE -> Gender.MALE.name
                Gender.FEMALE -> Gender.FEMALE.name
                Gender.UNKNOWN -> Gender.UNKNOWN.name
            },
            activityLevel = when (activityLevel) {
                ActivityLevel.SEDENTARY -> ActivityLevel.SEDENTARY.name
                ActivityLevel.LIGHT -> ActivityLevel.LIGHT.name
                ActivityLevel.MODERATE -> ActivityLevel.MODERATE.name
                ActivityLevel.VERY_ACTIVE -> ActivityLevel.VERY_ACTIVE.name
                ActivityLevel.UNKNOWN -> ActivityLevel.UNKNOWN.name
            },
            goalType = when (goalType) {
                GoalType.LOSE_WEIGHT -> GoalType.LOSE_WEIGHT.name
                GoalType.MAINTAIN -> GoalType.MAINTAIN.name
                GoalType.GAIN_WEIGHT -> GoalType.GAIN_WEIGHT.name
                GoalType.UNKNOWN -> GoalType.UNKNOWN.name
            },
            targetCalories = nutritionGoal.targetCalories,
            targetProteinsGrams = nutritionGoal.targetProteinsGrams,
            targetFatsGrams = nutritionGoal.targetFatsGrams,
            targetCarbsGrams = nutritionGoal.targetCarbsGrams,
            targetWeightKg = nutritionGoal.targetWeightKg,
            weeklyBudget = weeklyBudget,
            updatedAtEpochMs = updatedAt.toEpochMilliseconds(),
        )
    }

    private fun String.toGender(): Gender =
        Gender.entries.firstOrNull { it.name == this } ?: Gender.UNKNOWN

    private fun String.toActivityLevel(): ActivityLevel =
        ActivityLevel.entries.firstOrNull { it.name == this } ?: ActivityLevel.UNKNOWN

    private fun String.toGoalType(): GoalType =
        GoalType.entries.firstOrNull { it.name == this } ?: GoalType.UNKNOWN
}
