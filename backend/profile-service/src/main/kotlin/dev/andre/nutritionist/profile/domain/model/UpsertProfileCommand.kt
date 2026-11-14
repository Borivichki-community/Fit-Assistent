package dev.andre.nutritionist.profile.domain.model

import java.math.BigDecimal

data class UpsertProfileCommand(
    val userId: UserId,
    val heightCm: Int,
    val weightKg: BigDecimal,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val nutritionGoal: NutritionGoal?,
    val weeklyBudget: BigDecimal,
)
