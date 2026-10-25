package dev.andre.nutritionist.domain.model

data class NutritionGoal(
    val targetCalories: Int,
    val targetProteinsGrams: Double,
    val targetFatsGrams: Double,
    val targetCarbsGrams: Double,
    val targetWeightKg: Double,
)