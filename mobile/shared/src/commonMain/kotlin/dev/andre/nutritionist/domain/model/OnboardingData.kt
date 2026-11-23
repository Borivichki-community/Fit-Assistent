package dev.andre.nutritionist.domain.model

/**
 * Иммутабельные данные сессии онбординга (регистрации) пользователя.
 */
data class OnboardingData(
    val firstName: String = "",
    val lastName: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val age: String = "",
    val gender: Gender = Gender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val weeklyBudget: String = "5000",
    val goalType: GoalType = GoalType.LOSE_WEIGHT,
)
