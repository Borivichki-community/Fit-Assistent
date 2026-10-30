package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.UserId

sealed interface ProfileNews {
    data class NavigateToEdit(val userId: UserId) : ProfileNews
    data class ShowSnackbar(val message: String) : ProfileNews
}