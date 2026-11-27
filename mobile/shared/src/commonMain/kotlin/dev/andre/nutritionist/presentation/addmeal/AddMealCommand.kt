package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.Recipe

/**
 * Асинхронные команды меню выбора блюда (@fitapp_4).
 */
sealed interface AddMealCommand {
    data class Search(
        val query: String,
        val forceRefresh: Boolean = false,
    ) : AddMealCommand

    data class AddProduct(
        val product: Product,
        val mealType: MealType,
        val dateEpochMs: Long,
    ) : AddMealCommand

    data class AddRecipe(
        val recipe: Recipe,
        val mealType: MealType,
        val dateEpochMs: Long,
    ) : AddMealCommand
}
