package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId

data class ProfileState(
    val content: Content = Content.Loading,
    val currentUserId: UserId? = null
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val profile: Profile) : Content
        data class Error(val message: String) : Content
    }
}