package dev.andre.nutritionist.presentation.airecipe

sealed interface MenuNews {
    data class ShowSnackbar(val message: String) : MenuNews
}
