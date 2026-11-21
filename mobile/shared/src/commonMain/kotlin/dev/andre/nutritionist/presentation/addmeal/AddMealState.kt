package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.FoodItem
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Неделимый снимок состояния экрана / шторки добавления блюда (Add Meal BottomSheet).
 */
data class AddMealState(
    val mealType: MealType = MealType.BREAKFAST,
    val userId: UserId? = null,
    val date: LocalDate? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "Все",
    val selectedFoodItem: FoodItem? = null,
    val portionGrams: Double = 100.0,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val foods: List<FoodItem>) : Content
        data class Error(val message: String) : Content
    }
}
