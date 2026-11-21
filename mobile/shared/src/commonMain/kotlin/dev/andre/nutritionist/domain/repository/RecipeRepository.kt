package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий рецептов блюд и AI-генерации.
 */
interface RecipeRepository {
    suspend fun generateRecipe(prompt: AiRecipePrompt): Result<Recipe>
    suspend fun getSavedRecipes(): Result<List<Recipe>>
    fun observeSavedRecipes(): Flow<List<Recipe>>
    suspend fun toggleFavorite(recipeId: RecipeId): Result<Recipe>
    suspend fun saveRecipe(recipe: Recipe): Result<Recipe>
}
