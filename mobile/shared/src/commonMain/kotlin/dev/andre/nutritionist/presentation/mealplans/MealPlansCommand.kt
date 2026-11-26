package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.UserId

sealed interface MealPlansCommand {
    data class LoadAll(
        val query: String,
        val initialPlanId: MealPlanTemplateId?,
    ) : MealPlansCommand

    data class CreatePlan(
        val authorId: UserId,
        val isCoach: Boolean,
        val title: String,
        val description: String,
        val durationDays: Int,
        val accessMode: AccessMode,
        val priceMinor: Long,
        val visibility: ContentVisibility,
        val starterRecipe: Recipe?,
    ) : MealPlansCommand

    data class PublishPlan(val plan: MealPlanTemplate) : MealPlansCommand
    data class LoadPlanDetails(val planId: MealPlanTemplateId) : MealPlansCommand
    data class SubscribeToPlan(val userId: UserId, val planId: MealPlanTemplateId) : MealPlansCommand
    data class ApplyPlanDay(val plan: MealPlanTemplate, val dayNumber: Int) : MealPlansCommand
    data class PublishRecipe(val recipeId: RecipeId) : MealPlansCommand
    data class ForkRecipe(val recipeId: RecipeId) : MealPlansCommand
    data class LogRecipeToDiary(val recipe: Recipe) : MealPlansCommand
    data class CreateQuickRecipe(val authorId: UserId, val isCoach: Boolean) : MealPlansCommand
}
