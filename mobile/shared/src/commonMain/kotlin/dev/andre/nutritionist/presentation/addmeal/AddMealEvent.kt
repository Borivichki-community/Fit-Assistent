package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.FoodItem
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Все события экрана добавления блюда в дневник питания.
 */
sealed interface AddMealEvent {

    sealed interface Ui : AddMealEvent {
        data class Init(
            val mealType: MealType,
            val userId: UserId,
            val date: LocalDate,
        ) : Ui

        data class SearchQueryChanged(val query: String) : Ui

        data class CategorySelected(val category: String) : Ui

        data class FoodSelected(val food: FoodItem) : Ui

        data class PortionChanged(val grams: Double) : Ui

        data object ConfirmAdd : Ui

        data object Dismiss : Ui

        data object BarcodeScanClick : Ui

        data object AiCameraClick : Ui

        data class NetworkStatusChanged(val isConnected: Boolean) : Ui
    }

    sealed interface Internal : AddMealEvent {
        data class FoodsLoaded(val foods: List<FoodItem>) : Internal

        data class SearchFailed(val error: Throwable) : Internal

        data class EntrySaved(val entry: DiaryEntry) : Internal

        data class SaveFailed(val error: Throwable) : Internal
    }
}
