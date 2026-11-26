package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.UserId

sealed interface MealPlansEvent {
    sealed interface Ui : MealPlansEvent {
        data class Load(
            val userId: UserId,
            val isCoach: Boolean,
            val initialPlanId: MealPlanTemplateId? = null,
        ) : Ui

        data object Retry : Ui
        data class TitleChanged(val value: String) : Ui
        data class DescriptionChanged(val value: String) : Ui
        data class DurationDaysSelected(val days: Int) : Ui
        data class AccessModeSelected(val mode: AccessMode) : Ui
        data class PriceRubChanged(val value: String) : Ui
        data class SearchQueryChanged(val value: String) : Ui
        data object SearchClicked : Ui
        data object CreatePlanClicked : Ui
        data class PublishPlanClicked(val planId: MealPlanTemplateId) : Ui
        data class OpenPlanDetailsClicked(val planId: MealPlanTemplateId) : Ui
        data class SubscribeToPlanClicked(val planId: MealPlanTemplateId) : Ui
        data class ApplyPlanDayClicked(val planId: MealPlanTemplateId, val dayNumber: Int = 1) : Ui
        data class PublishRecipeClicked(val recipeId: RecipeId) : Ui
        data class ForkRecipeClicked(val recipeId: RecipeId) : Ui
        data class LogRecipeToDiaryClicked(val recipeId: RecipeId) : Ui
        data object CreateQuickRecipeClicked : Ui
    }

    sealed interface Internal : MealPlansEvent {
        data class DataLoaded(
            val plans: List<MealPlanTemplate>,
            val selectedPlan: MealPlanTemplate?,
            val recipes: List<Recipe>,
        ) : Internal

        data class LoadFailed(val error: Throwable) : Internal
        data class PlanSaved(val plan: MealPlanTemplate, val message: String) : Internal
        data class PlanDetailsLoaded(val plan: MealPlanTemplate) : Internal
        data class RecipeUpdated(val recipe: Recipe, val message: String) : Internal
        data class ActionSucceeded(val message: String) : Internal
        data class ActionFailed(val error: Throwable) : Internal
    }
}
