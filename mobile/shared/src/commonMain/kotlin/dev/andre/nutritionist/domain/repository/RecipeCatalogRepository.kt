package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import kotlinx.coroutines.flow.Flow

interface RecipeCatalogRepository {
    fun observeMyRecipes(): Flow<List<Recipe>>

    suspend fun searchPublicRecipes(
        criteria: RecipeSearchCriteria = RecipeSearchCriteria(),
    ): Result<PagedResult<Recipe>>

    suspend fun getRecipeById(id: RecipeId): Result<Recipe>

    suspend fun saveRecipe(recipe: Recipe): Result<Recipe>

    suspend fun deleteRecipe(id: RecipeId): Result<Unit>

    suspend fun forkRecipe(
        originId: RecipeId,
        newTitle: String? = null,
    ): Result<Recipe>

    suspend fun publishRecipe(
        id: RecipeId,
        visibility: ContentVisibility,
    ): Result<Recipe>
}
