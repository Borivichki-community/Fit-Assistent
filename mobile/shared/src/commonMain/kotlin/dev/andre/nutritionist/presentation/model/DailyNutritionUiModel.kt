package dev.andre.nutritionist.presentation.model

data class DailyNutritionUiModel(
    val targetCaloriesLabel: String = "2 000 ккал",
    val consumedCaloriesLabel: String = "0 ккал",
    val remainingCaloriesLabel: String = "2 000 ккал",
    val caloriesProgress: Float = 0f,
    val caloriesProgressLabel: String = "0%",
    val isCaloriesExceeded: Boolean = false,
    val proteinsConsumedLabel: String = "0 г",
    val proteinsTargetLabel: String = "120 г",
    val proteinsProgress: Float = 0f,
    val fatsConsumedLabel: String = "0 г",
    val fatsTargetLabel: String = "70 г",
    val fatsProgress: Float = 0f,
    val carbsConsumedLabel: String = "0 г",
    val carbsTargetLabel: String = "250 г",
    val carbsProgress: Float = 0f,
)
