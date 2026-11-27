package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.MealType

/**
 * UI-состояние модального меню выбора блюда (@fitapp_4).
 */
data class AddMealUiState(
    val mealType: MealType = MealType.LUNCH,
    val mealTypeTitle: String = "Дневной приём",
    val query: String = "",
    val items: List<AddMealSearchItemUiModel> = emptyList(),
    val content: Content = Content.Idle,
) {
    sealed interface Content {
        data object Idle : Content
        data object Loading : Content
        data class Success(val isSaving: Boolean = false) : Content
        data object Empty : Content
        data class Error(val message: String) : Content
    }
}
