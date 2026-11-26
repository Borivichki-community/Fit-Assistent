package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.PagedResult
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeSearchCriteria
import dev.andre.nutritionist.domain.repository.RecipeCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeRecipeCatalogRepository : RecipeCatalogRepository {

    // mutable by design: test control
    val publicRecipes = mutableListOf<Recipe>()
    private val myRecipesFlow = MutableStateFlow<List<Recipe>>(emptyList())
    var shouldThrow: Throwable? = null
    var searchCallCount: Int = 0

    override fun observeMyRecipes(): Flow<List<Recipe>> = myRecipesFlow.asStateFlow()

    override suspend fun searchPublicRecipes(criteria: RecipeSearchCriteria): Result<PagedResult<Recipe>> {
        searchCallCount++
        val error = shouldThrow
        if (error != null) return Result.failure(error)

        val filtered = publicRecipes.filter { recipe ->
            criteria.query.isBlank() || recipe.title.contains(criteria.query, ignoreCase = true)
        }
        return Result.success(PagedResult(items = filtered))
    }

    override suspend fun getRecipeById(id: RecipeId): Result<Recipe> {
        val error = shouldThrow
        if (error != null) return Result.failure(error)
        val found = publicRecipes.firstOrNull { it.id == id }
            ?: myRecipesFlow.value.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException("Recipe $id not found"))
        return Result.success(found)
    }

    override suspend fun saveRecipe(recipe: Recipe): Result<Recipe> {
        val error = shouldThrow
        if (error != null) return Result.failure(error)
        myRecipesFlow.value = myRecipesFlow.value.filterNot { it.id == recipe.id } + recipe
        return Result.success(recipe)
    }

    override suspend fun deleteRecipe(id: RecipeId): Result<Unit> {
        myRecipesFlow.value = myRecipesFlow.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun forkRecipe(originId: RecipeId, newTitle: String?): Result<Recipe> =
        getRecipeById(originId).map { origin ->
            origin.copy(
                id = RecipeId("fork-${origin.id.raw}"),
                title = newTitle ?: origin.title,
            )
        }

    override suspend fun publishRecipe(id: RecipeId, visibility: ContentVisibility): Result<Recipe> =
        getRecipeById(id).map { it.copy(visibility = visibility) }

    fun setMyRecipes(recipes: List<Recipe>) {
        myRecipesFlow.value = recipes
    }

    fun reset() {
        publicRecipes.clear()
        myRecipesFlow.value = emptyList()
        shouldThrow = null
        searchCallCount = 0
    }
}
