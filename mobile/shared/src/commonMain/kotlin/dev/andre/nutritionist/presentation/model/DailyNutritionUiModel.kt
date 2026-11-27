package dev.andre.nutritionist.presentation.model

import dev.andre.nutritionist.domain.model.NutritionFacts

data class DailyNutritionUiModel(
    val targetCaloriesLabel: String = "2 000 ккал",
    val consumedCaloriesLabel: String = "0 ккал",
    val remainingCaloriesLabel: String = "2 000 ккал",
    val caloriesConsumedValue: String = "0",
    val caloriesTargetValue: String = "/ 2 000",
    val caloriesProgress: Float = 0f,
    val caloriesProgressLabel: String = "0%",
    val isCaloriesExceeded: Boolean = false,
    val proteinsConsumedLabel: String = "0 г",
    val proteinsTargetLabel: String = "120 г",
    val proteinsConsumedValue: String = "0",
    val proteinsTargetShortLabel: String = "/ 120 г",
    val proteinsProgress: Float = 0f,
    val fatsConsumedLabel: String = "0 г",
    val fatsTargetLabel: String = "70 г",
    val fatsConsumedValue: String = "0",
    val fatsTargetShortLabel: String = "/ 70 г",
    val fatsProgress: Float = 0f,
    val carbsConsumedLabel: String = "0 г",
    val carbsTargetLabel: String = "250 г",
    val carbsConsumedValue: String = "0",
    val carbsTargetShortLabel: String = "/ 250 г",
    val carbsProgress: Float = 0f,
    val totalMealCostLabel: String = "0 ₽",
    val remainingNutrition: NutritionFacts = NutritionFacts.ZERO,
    val dailyTargetCalories: Int = 2000,
    val dailyBudgetLabel: String = "Бюджет: ~0 ₽",
)
