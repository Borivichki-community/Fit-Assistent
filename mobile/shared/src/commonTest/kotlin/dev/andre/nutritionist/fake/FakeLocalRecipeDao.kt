package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.local.dao.LocalRecipeDao
import dev.andre.nutritionist.data.local.entity.LocalRecipeEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeIngredientEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeWithIngredients
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLocalRecipeDao : LocalRecipeDao {

    // mutable by design: test double in-memory backing store
    val recipes = linkedMapOf<String, LocalRecipeEntity>()
    val ingredientsByRecipe = linkedMapOf<String, List<LocalRecipeIngredientEntity>>()
    private val stateFlow = MutableStateFlow<List<LocalRecipeWithIngredients>>(emptyList())

    // mutable by design: test failure injection
    var shouldThrow: Throwable? = null

    override fun observeMyRecipes(): Flow<List<LocalRecipeWithIngredients>> =
        stateFlow.map { list ->
            list.filter { !it.recipe.isDeleted && it.recipe.authorType != "SYSTEM" }
                .sortedByDescending { it.recipe.updatedAtEpochMs }
        }

    override fun observeAllActive(): Flow<List<LocalRecipeWithIngredients>> =
        stateFlow.map { list ->
            list.filter { !it.recipe.isDeleted }
                .sortedByDescending { it.recipe.updatedAtEpochMs }
        }

    override suspend fun getPendingRecipes(): List<LocalRecipeWithIngredients> {
        shouldThrow?.let { throw it }
        return recipes.values
            .filter { !it.isDeleted && it.syncStatus == "PENDING" }
            .map { entity -> LocalRecipeWithIngredients(entity, ingredientsByRecipe[entity.id].orEmpty()) }
    }

    override suspend fun getRecipeWithIngredients(id: String): LocalRecipeWithIngredients? {
        shouldThrow?.let { throw it }
        val entity = recipes[id] ?: return null
        if (entity.isDeleted) return null
        return LocalRecipeWithIngredients(entity, ingredientsByRecipe[id].orEmpty())
    }

    override suspend fun getRawById(id: String): LocalRecipeEntity? {
        shouldThrow?.let { throw it }
        return recipes[id]
    }

    override suspend fun upsertRecipe(recipe: LocalRecipeEntity) {
        shouldThrow?.let { throw it }
        recipes[recipe.id] = recipe
        emitSnapshot()
    }

    override suspend fun insertIngredients(ingredients: List<LocalRecipeIngredientEntity>) {
        shouldThrow?.let { throw it }
        ingredients.groupBy { it.recipeId }.forEach { (recipeId, list) ->
            ingredientsByRecipe[recipeId] = list
        }
        emitSnapshot()
    }

    override suspend fun deleteIngredientsByRecipeId(recipeId: String) {
        shouldThrow?.let { throw it }
        ingredientsByRecipe.remove(recipeId)
        emitSnapshot()
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        shouldThrow?.let { throw it }
        val current = recipes[id] ?: return
        recipes[id] = current.copy(syncStatus = status)
        emitSnapshot()
    }

    override suspend fun markDeleted(id: String, status: String, updatedAt: Long) {
        shouldThrow?.let { throw it }
        val current = recipes[id] ?: return
        recipes[id] = current.copy(isDeleted = true, syncStatus = status, updatedAtEpochMs = updatedAt)
        emitSnapshot()
    }

    override suspend fun hardDeleteById(id: String) {
        shouldThrow?.let { throw it }
        recipes.remove(id)
        ingredientsByRecipe.remove(id)
        emitSnapshot()
    }

    private fun emitSnapshot() {
        stateFlow.value = recipes.values.map { entity ->
            LocalRecipeWithIngredients(entity, ingredientsByRecipe[entity.id].orEmpty())
        }
    }

    fun reset() {
        recipes.clear()
        ingredientsByRecipe.clear()
        stateFlow.value = emptyList()
        shouldThrow = null
    }
}
