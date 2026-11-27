package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan>
    fun streamWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String> = emptyList(),
        preferredProductIds: List<String> = emptyList(),
    ): Flow<MenuGenerationProgress>

    suspend fun saveRecipe(recipe: Recipe): Result<Unit>
    suspend fun deleteSavedRecipe(id: RecipeId): Result<Unit>
    fun observeSavedRecipes(): Flow<List<Recipe>>
}
