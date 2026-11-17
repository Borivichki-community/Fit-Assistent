package dev.andre.nutritionist.profile.domain.model

data class DailyCaloriesBalance(
    val targetCalories: Int,
    val consumedCalories: Long,
    val remainingCalories: Long,
)
