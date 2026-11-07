package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId

data class ProfileState(
    val content: Content = Content.Loading,
    val currentUserId: UserId? = null,
    val isOffline: Boolean = false,
) {
    sealed interface Content {
        data object Loading : Content

        data class Form(
            val name: String = "",
            val age: String = "",
            val targetWeight: String = "",
            val dailyCalories: String = "",
        ) : Content

        data class Success(val profile: Profile) : Content

        data class Error(val message: String) : Content

        data class ValidationError(
            val name: String = "",
            val age: String = "",
            val targetWeight: String = "",
            val dailyCalories: String = "",
            val nameError: String? = null,
            val ageError: String? = null,
            val targetWeightError: String? = null,
            val dailyCaloriesError: String? = null,
        ) : Content
    }
}
