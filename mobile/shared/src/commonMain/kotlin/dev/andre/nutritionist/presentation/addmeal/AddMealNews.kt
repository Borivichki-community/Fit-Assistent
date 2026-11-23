package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.DiaryEntry

/**
 * Одноразовые эффекты экрана добавления блюда в дневник питания.
 */
sealed interface AddMealNews {
    data class MealAddedSuccess(val entry: DiaryEntry) : AddMealNews
    data object Dismiss : AddMealNews
    data class ShowSnackbar(val message: String) : AddMealNews
    data object OpenBarcodeScanner : AddMealNews
    data object OpenAiCamera : AddMealNews
}
