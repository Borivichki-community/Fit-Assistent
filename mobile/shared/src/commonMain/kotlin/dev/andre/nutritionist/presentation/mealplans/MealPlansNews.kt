package dev.andre.nutritionist.presentation.mealplans

sealed interface MealPlansNews {
    data class ShowSnackbar(val message: String) : MealPlansNews
}
