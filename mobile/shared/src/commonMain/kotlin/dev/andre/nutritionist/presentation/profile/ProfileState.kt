package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.model.UserRole

data class ProfileState(
    val content: Content = Content.Loading,
    val currentUserId: UserId? = null,
    val isOffline: Boolean = false,
    val isSaving: Boolean = false,
    val editDraft: Profile? = null,
    val displayName: String? = null,
    val age: String? = null,
    val email: String? = null,
    val userRole: UserRole? = null,
) {
    sealed interface Content {
        data object Loading : Content

        data class Form(
            val name: String = "",
            val age: String = "",
            val targetWeight: String = "",
            val dailyCalories: String = "",
        ) : Content

        data class Success(
            val profile: Profile,
            val displayName: String? = null,
            val age: String? = null,
        ) : Content

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
