package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.repository.RecipeRepository
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeRecipeRepository : RecipeRepository {

    // mutable by design: test control
    var planToReturn: MealPlan? = null
    var streamEventsToEmit: List<MenuGenerationProgress> = emptyList()
    var shouldThrow: Throwable? = null
    var generateCallCount: Int = 0

    override suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan> {
        generateCallCount++
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

    fun reset() {
        planToReturn = null
        streamEventsToEmit = emptyList()
        shouldThrow = null
        generateCallCount = 0
    }
}
