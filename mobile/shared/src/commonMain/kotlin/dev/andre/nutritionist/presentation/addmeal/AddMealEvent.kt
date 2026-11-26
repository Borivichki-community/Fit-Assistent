package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.Recipe

/**
 * События меню выбора продукта или рецепта (@fitapp_4).
 */
sealed interface AddMealEvent {

    sealed interface Ui : AddMealEvent {
        data class Open(
            val mealType: MealType,
            val dateEpochMs: Long,
        ) : Ui

        data class QueryChanged(val query: String) : Ui
        data object ClearQuery : Ui
        data object Retry : Ui
        data class ItemClicked(
            val id: String,
            val kind: AddMealItemKind,
        ) : Ui

        data object Dismiss : Ui
        data object AiGenerateClicked : Ui
        data object TakePhotoClicked : Ui
        data object VoiceAssistantClicked : Ui
        data object SavedFoodClicked : Ui
    }

    sealed interface Internal : AddMealEvent {
        data class SearchLoaded(
            val query: String,
            val products: List<Product>,
            val recipes: List<Recipe>,
        ) : Internal

        data class SearchFailed(val error: Throwable) : Internal
        data class ItemSaved(val record: MealRecord) : Internal
        data class SaveFailed(val error: Throwable) : Internal
    }
}
