package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.presentation.airecipe.RecipeIngredientUiModel
import dev.andre.nutritionist.presentation.airecipe.RecipeResultUiModel
import dev.andre.nutritionist.presentation.airecipe.RecipeStepUiModel

/**
 * Маппер MVI-состояния экрана сохранённых рецептов в [SavedRecipesUiState].
 */
class SavedRecipesUiStateMapper : UiStateMapper<SavedRecipesState, SavedRecipesUiState> {

    override fun map(state: SavedRecipesState): SavedRecipesUiState {
        val filter = state.filter
        val filters = listOf(
            MealTypeFilterUiModel(
                mealType = null,
                title = "Все",
                isSelected = filter.selectedMealType == null,
            ),
            MealTypeFilterUiModel(
                mealType = MealType.BREAKFAST,
                title = "Завтрак",
                isSelected = filter.selectedMealType == MealType.BREAKFAST,
            ),
            MealTypeFilterUiModel(
                mealType = MealType.LUNCH,
                title = "Обед",
                isSelected = filter.selectedMealType == MealType.LUNCH,
            ),
            MealTypeFilterUiModel(
                mealType = MealType.DINNER,
                title = "Ужин",
                isSelected = filter.selectedMealType == MealType.DINNER,
            ),
            MealTypeFilterUiModel(
                mealType = MealType.SNACK,
                title = "Перекус",
                isSelected = filter.selectedMealType == MealType.SNACK,
            ),
        )

        val content = when (val c = state.content) {
            is SavedRecipesState.Content.Loading -> SavedRecipesUiState.Content.Loading

            is SavedRecipesState.Content.Empty -> SavedRecipesUiState.Content.Empty(
                title = "Коллекция рецептов пуста",
                description = "Сохраняйте любимые блюда из каталога или сгенерируйте авторский рецепт с помощью AI.",
                actionTitle = "Сгенерировать с AI",
            )

            is SavedRecipesState.Content.Error -> SavedRecipesUiState.Content.Error(c.message)

            is SavedRecipesState.Content.Success -> {
                val filtered = c.allRecipes.filter { recipe ->
                    val matchesQuery = filter.query.isBlank() ||
                        recipe.title.contains(filter.query, ignoreCase = true) ||
                        recipe.description.contains(filter.query, ignoreCase = true)

                    val matchesMealType = filter.selectedMealType == null || recipe.mealType == filter.selectedMealType

                    matchesQuery && matchesMealType
                }

                if (filtered.isEmpty()) {
                    SavedRecipesUiState.Content.Empty(
                        title = "Ничего не найдено",
                        description = "Попробуйте изменить поисковый запрос или сбросить фильтр приёма пищи.",
                        actionTitle = "Сбросить фильтры",
                    )
                } else {
                    SavedRecipesUiState.Content.Success(
                        recipes = filtered.map { mapToItem(it) },
                        countText = "${filtered.size} рецептов",
                        selectedRecipeDetail = c.selectedRecipe?.let { mapToDetail(it) },
                    )
                }
            }
        }

        return SavedRecipesUiState(
            searchQuery = filter.query,
            mealTypeFilters = filters,
            content = content,
        )
    }

    private fun mapToItem(recipe: Recipe): SavedRecipeItemUiModel {
        return SavedRecipeItemUiModel(
            id = recipe.id.raw,
            title = recipe.title,
            description = recipe.description,
            mealTypeBadge = formatMealType(recipe.mealType),
            caloriesBadge = "${recipe.calories} ккал",
            budgetBadge = "~${recipe.estimatedCostRub} ₽",
            cookingTimeBadge = "${recipe.cookingTimeMinutes} мин",
            macroSummaryText = "Б: ${recipe.proteins.toInt()}г • Ж: ${recipe.fats.toInt()}г • У: ${recipe.carbs.toInt()}г",
            isFavorite = recipe.isFavorite,
        )
    }

    private fun mapToDetail(recipe: Recipe): RecipeResultUiModel {
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
            isSaved = recipe.isFavorite,
        )
    }

    private fun formatMealType(mealType: MealType): String = when (mealType) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
        MealType.SNACK -> "Перекус"
    }
}
