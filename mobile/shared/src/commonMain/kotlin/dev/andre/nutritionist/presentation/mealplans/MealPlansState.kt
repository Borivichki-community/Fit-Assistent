package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.UserId

data class MealPlansState(
    val currentUserId: UserId = UserId(""),
    val isCoach: Boolean = false,
    val initialPlanId: MealPlanTemplateId? = null,
    val titleInput: String = "",
    val descriptionInput: String = "",
    val durationDays: Int = 7,
    val accessMode: AccessMode = AccessMode.FREE,
    val priceRubInput: String = "990",
    val visibility: ContentVisibility = ContentVisibility.PRIVATE,
    val searchQuery: String = "",
    val plans: List<MealPlanTemplate> = emptyList(),
    val selectedPlan: MealPlanTemplate? = null,
    val recipes: List<Recipe> = emptyList(),
    val isSubmitting: Boolean = false,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data object Loaded : Content
        data class Error(val message: String) : Content
    }
}
