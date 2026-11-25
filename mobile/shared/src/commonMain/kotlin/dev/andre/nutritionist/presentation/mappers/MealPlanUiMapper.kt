package dev.andre.nutritionist.presentation.mappers

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.PlanId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.MealPlanRecipeUiModel
import dev.andre.nutritionist.presentation.model.MealPlanUiModel

object MealPlanUiMapper {

    fun MealPlan.toUiModel(): MealPlanUiModel = MealPlanUiModel(
        id = PlanId(id),
        period = period,
        periodLabel = period.toLabel(),
        meals = meals.map { it.toPlanRecipeUiModel() },
        totalCaloriesLabel = NutritionFormatter.formatCalories(totalNutrition.calories),
        totalProteinsLabel = NutritionFormatter.formatGram(totalNutrition.proteins),
        totalFatsLabel = NutritionFormatter.formatGram(totalNutrition.fats),
        totalCarbsLabel = NutritionFormatter.formatGram(totalNutrition.carbs),
        estimatedCostLabel = MoneyFormatter.format(estimatedCost),
        cookingInstructions = cookingInstructions,
    )

    private fun Recipe.toPlanRecipeUiModel(): MealPlanRecipeUiModel = MealPlanRecipeUiModel(
        id = id,
        title = title,
        mealTypeLabel = mealType.toLabel(),
        caloriesLabel = NutritionFormatter.formatCalories(nutrition.calories),
        proteinsLabel = NutritionFormatter.formatGram(nutrition.proteins),
        fatsLabel = NutritionFormatter.formatGram(nutrition.fats),
        carbsLabel = NutritionFormatter.formatGram(nutrition.carbs),
        costLabel = MoneyFormatter.format(estimatedCost),
        cookingTimeLabel = if (cookingTimeMinutes > 0) "$cookingTimeMinutes мин" else "",
    )

    private fun TargetPeriod.toLabel(): String = when (this) {
        TargetPeriod.UNSPECIFIED -> "Не указано"
        TargetPeriod.DAY -> "День"
        TargetPeriod.BREAKFAST -> "Завтрак"
        TargetPeriod.LUNCH -> "Обед"
        TargetPeriod.DINNER -> "Ужин"
        TargetPeriod.SNACK -> "Перекус"
    }

    private fun MealType.toLabel(): String = when (this) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
        MealType.SNACK -> "Перекус"
    }
}
