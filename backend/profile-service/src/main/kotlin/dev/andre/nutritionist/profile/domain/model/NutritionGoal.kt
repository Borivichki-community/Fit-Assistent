package dev.andre.nutritionist.profile.domain.model

import java.math.BigDecimal

data class NutritionGoal(
    val targetCalories: Int,
    val targetProteinsGrams: BigDecimal,
    val targetFatsGrams: BigDecimal,
    val targetCarbsGrams: BigDecimal,
    val targetWeightKg: BigDecimal,
)
