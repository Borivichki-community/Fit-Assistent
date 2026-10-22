package dev.andre.nutritionist.domain.model

import kotlinx.datetime.Instant

data class Profile(
    val id: ProfileId,
    val userId: UserId,
    val heightCm: Int,
    val weightKg: Double,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val nutritionGoal: NutritionGoal,
    val updatedAt: Instant,
)