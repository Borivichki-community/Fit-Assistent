package dev.andre.nutritionist.diary.domain.model

data class MealNutritionSummary(
    val mealType: MealType,
    val nutrition: NutritionSummary,
)
