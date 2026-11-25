package dev.andre.nutritionist.presentation.model

import dev.andre.nutritionist.domain.model.PlanId
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.TargetPeriod

data class MealPlanRecipeUiModel(
    val id: RecipeId,
    val title: String,
    val mealTypeLabel: String,
    val caloriesLabel: String,
    val proteinsLabel: String,
    val fatsLabel: String,
    val carbsLabel: String,
    val costLabel: String,
    val cookingTimeLabel: String,
)

data class MealPlanUiModel(
    val id: PlanId,
    val period: TargetPeriod,
    val periodLabel: String,
    val meals: List<MealPlanRecipeUiModel>,
    val totalCaloriesLabel: String,
    val totalProteinsLabel: String,
    val totalFatsLabel: String,
    val totalCarbsLabel: String,
    val estimatedCostLabel: String,
    val cookingInstructions: String?,
)
