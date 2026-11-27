package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.RecipeId

data class MealPlanCardUiModel(
    val id: MealPlanTemplateId,
    val title: String,
    val metaLabel: String,
    val idLabel: String,
    val isPublic: Boolean,
    val hasItemsForDayOne: Boolean,
)

data class PublicRecipeCardUiModel(
    val id: RecipeId,
    val title: String,
    val metaLabel: String,
    val isPublic: Boolean,
)

data class MealPlansUiState(
    val isCoach: Boolean = false,
    val titleInput: String = "",
    val descriptionInput: String = "",
    val durationDays: Int = 7,
    val selectedAccessMode: AccessMode = AccessMode.FREE,
    val priceRubInput: String = "990",
    val searchQuery: String = "",
    val isSubmitting: Boolean = false,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(
            val selectedPlanTitle: String?,
            val selectedPlanDetail: String?,
            val plans: List<MealPlanCardUiModel>,
            val recipes: List<PublicRecipeCardUiModel>,
        ) : Content

        data class Error(val message: String) : Content
    }
}
