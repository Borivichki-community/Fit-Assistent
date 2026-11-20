package dev.andre.nutritionist.domain.model

data class NutritionGoal(
    val targetCalories: Int = 0,
    val targetProteinsGrams: Double = 0.0,
    val targetFatsGrams: Double = 0.0,
    val targetCarbsGrams: Double = 0.0,
    val targetWeightKg: Double = 0.0,
) {
    companion object {
        val EMPTY = NutritionGoal(
            targetCalories = 0,
            targetProteinsGrams = 0.0,
            targetFatsGrams = 0.0,
            targetCarbsGrams = 0.0,
            targetWeightKg = 0.0,
        )
    }
}
