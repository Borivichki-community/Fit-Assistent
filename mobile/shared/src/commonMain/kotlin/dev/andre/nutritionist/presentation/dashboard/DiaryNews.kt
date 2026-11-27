package dev.andre.nutritionist.presentation.dashboard

sealed interface DiaryNews {
    data class ShowSnackbar(val message: String) : DiaryNews
}
