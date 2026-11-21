package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe

/**
 * Чистый маппер состояния [AiRecipeState] в [AiRecipeUiState].
 */
class AiRecipeUiStateMapper {

    private val availablePreferences = listOf("Все", "Высокий белок", "Эконом", "Вегетарианское", "Без глютена")

    fun map(state: AiRecipeState): AiRecipeUiState {
        val content = when (val c = state.content) {
            is AiRecipeState.Content.Configuring -> {
                val prompt = c.prompt
                AiRecipeUiState.Content.Configuring(
                    prompt = AiRecipePromptUiModel(
                        query = prompt.query,
                        targetCalories = prompt.targetCalories,
                        targetCaloriesText = "${prompt.targetCalories} ккал",
                        targetBudgetRub = prompt.targetBudgetRub,
                        targetBudgetText = "${prompt.targetBudgetRub} ₽",
                        mealType = prompt.mealType,
                        mealTypeTitle = formatMealType(prompt.mealType),
                        dietaryPreference = prompt.dietaryPreference,
                        availablePreferences = availablePreferences,
                    ),
                )
            }
            is AiRecipeState.Content.Generating -> {
                AiRecipeUiState.Content.Generating(
                    generating = AiRecipeGeneratingUiModel(
                        phaseText = c.phaseText,
                        progress = c.progress,
                        progressPercentText = "${(c.progress * 100).toInt()}%",
                    ),
                )
            }
            is AiRecipeState.Content.Success -> {
                AiRecipeUiState.Content.Success(
                    recipe = mapRecipe(c.recipe, c.isSaved),
                )
            }
            is AiRecipeState.Content.Error -> {
                AiRecipeUiState.Content.Error(c.message)
            }
        }
        return AiRecipeUiState(
            content = content,
            isOnline = state.isOnline,
        )
    }

    private fun mapRecipe(recipe: Recipe, isSaved: Boolean): RecipeResultUiModel {
        val totalMacroGrams = (recipe.proteins + recipe.fats + recipe.carbs).coerceAtLeast(1.0)
        return RecipeResultUiModel(
            id = recipe.id.raw,
            title = recipe.title,
            description = recipe.description,
            mealTypeBadge = formatMealType(recipe.mealType),
            caloriesBadge = "${recipe.calories} ккал",
            budgetBadge = "~${recipe.estimatedCostRub} ₽",
            cookingTimeBadge = "${recipe.cookingTimeMinutes} мин",
            macroSummaryText = "Б: ${recipe.proteins.toInt()}г • Ж: ${recipe.fats.toInt()}г • У: ${recipe.carbs.toInt()}г",
            proteinText = "${recipe.proteins.toInt()} г белка",
            fatsText = "${recipe.fats.toInt()} г жиров",
            carbsText = "${recipe.carbs.toInt()} г углев.",
            proteinFraction = (recipe.proteins / totalMacroGrams).toFloat().coerceIn(0f, 1f),
            fatsFraction = (recipe.fats / totalMacroGrams).toFloat().coerceIn(0f, 1f),
            carbsFraction = (recipe.carbs / totalMacroGrams).toFloat().coerceIn(0f, 1f),
            ingredients = recipe.ingredients.map { RecipeIngredientUiModel(name = it.name, amount = it.amount) },
            steps = recipe.steps.mapIndexed { index, step -> RecipeStepUiModel(stepNumber = index + 1, instruction = step) },
            isSaved = isSaved || recipe.isFavorite,
        )
    }

    private fun formatMealType(mealType: MealType): String = when (mealType) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
        MealType.SNACK -> "Перекус"
    }
}
