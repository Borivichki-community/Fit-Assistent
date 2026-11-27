package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.repository.RecipeRepository
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class FakeRecipeRepository : RecipeRepository {

    // mutable by design: test control
    var planToReturn: MealPlan? = null
    var streamEventsToEmit: List<MenuGenerationProgress> = emptyList()
    var shouldThrow: Throwable? = null
    var generateCallCount: Int = 0
    var generateDelayMs: Long = 0L
    var generateCancelledCount: Int = 0
    private val savedRecipesFlow = MutableStateFlow<List<Recipe>>(emptyList())

    override suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan> {
        generateCallCount++
        if (generateDelayMs > 0L) {
            try {
                delay(generateDelayMs)
            } catch (e: kotlinx.coroutines.CancellationException) {
                generateCancelledCount++
                throw e
            }
        }
        val error = shouldThrow
        if (error != null) {
            return Result.failure(error)
        }
        val plan = planToReturn ?: error("planToReturn not configured in FakeRecipeRepository")
        return Result.success(plan)
    }

    override fun streamWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String>,
        preferredProductIds: List<String>,
    ): Flow<MenuGenerationProgress> = flow {
        val error = shouldThrow
        if (error != null) {
            throw error
        }
        for (event in streamEventsToEmit) {
            emit(event)
        }
    }

    override suspend fun saveRecipe(recipe: Recipe): Result<Unit> {
        val error = shouldThrow
        if (error != null) return Result.failure(error)
        savedRecipesFlow.value = listOf(recipe) + savedRecipesFlow.value.filterNot { it.id == recipe.id }
        return Result.success(Unit)
    }

    override suspend fun deleteSavedRecipe(id: RecipeId): Result<Unit> {
        val error = shouldThrow
        if (error != null) return Result.failure(error)
        savedRecipesFlow.value = savedRecipesFlow.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override fun observeSavedRecipes(): Flow<List<Recipe>> = savedRecipesFlow.asStateFlow()

    fun reset() {
        planToReturn = null
        streamEventsToEmit = emptyList()
        shouldThrow = null
        generateCallCount = 0
        generateDelayMs = 0L
        generateCancelledCount = 0
        savedRecipesFlow.value = emptyList()
    }
}
