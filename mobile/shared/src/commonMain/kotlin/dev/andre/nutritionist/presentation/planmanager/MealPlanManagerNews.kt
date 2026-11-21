package dev.andre.nutritionist.presentation.planmanager

/**
 * Одноразовые эффекты экрана менеджера планов питания.
 */
sealed interface MealPlanManagerNews {
    data class ShowSnackbar(val message: String) : MealPlanManagerNews
    data object NavigateToCatalog : MealPlanManagerNews
}
