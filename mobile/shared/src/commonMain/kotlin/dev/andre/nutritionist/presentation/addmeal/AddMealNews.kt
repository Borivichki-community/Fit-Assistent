package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.MealRecord

/**
 * Разовые UI-события меню выбора блюда (@fitapp_4).
 */
sealed interface AddMealNews {
    data class NavigateToAiGenerator(val mealType: dev.andre.nutritionist.domain.model.MealType, val dateEpochMs: Long, val targetCalories: Int) : AddMealNews
    data object Dismiss : AddMealNews
    data class MealAdded(val record: MealRecord) : AddMealNews
    data class ShowSnackbar(val message: String) : AddMealNews
}
