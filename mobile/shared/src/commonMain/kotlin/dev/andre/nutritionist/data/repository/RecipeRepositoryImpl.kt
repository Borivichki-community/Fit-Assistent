package dev.andre.nutritionist.data.repository

import dev.andre.nutritionist.data.local.dao.SavedRecipeDao
import dev.andre.nutritionist.data.mapper.SavedRecipeEntityMapper.toDomain
import dev.andre.nutritionist.data.mapper.SavedRecipeEntityMapper.toSavedEntity
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.repository.RecipeRepository
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecipeRepositoryImpl(
    private val remoteDataSource: RecipeRemoteDataSource,
    private val savedRecipeDao: SavedRecipeDao,
    private val dispatchers: CoroutineDispatchers,
) : RecipeRepository {

    override suspend fun generateMealPlan(params: GenerateBudgetMenuParams): Result<MealPlan> =
        withContext(dispatchers.io) {
            remoteDataSource.generateMealPlan(params)
        }

    override fun streamWeekMenu(
        dailyCalories: Int,
        weeklyBudgetMinor: Long,
        excludedProductIds: List<String>,
        preferredProductIds: List<String>,
    ): Flow<MenuGenerationProgress> = flow {
        emit(MenuGenerationProgress.Initializing)
        var lastDay: MealPlan? = null
        remoteDataSource.generateWeekMenu(
            dailyCalories = dailyCalories,
            weeklyBudgetMinor = weeklyBudgetMinor,
            excludedProductIds = excludedProductIds,
            preferredProductIds = preferredProductIds,
        ).collect { dayProto ->
            val dayPlan = MealPlan(
                id = "day-${dayProto.dayNumber}",
                period = TargetPeriod.DAY,
                meals = emptyList(),
                totalNutrition = NutritionFacts(calories = dayProto.calories),
                estimatedCost = Money.fromMinor(dayProto.estimatedCostMinor),
            )
            lastDay = dayPlan
            emit(MenuGenerationProgress.StreamingDay(dayProto.dayNumber, dayPlan))
        }
        val finalPlan = lastDay
        if (finalPlan != null) {
            emit(MenuGenerationProgress.Completed(finalPlan))
        }
    }.flowOn(dispatchers.io)

    override suspend fun saveRecipe(recipe: Recipe): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                savedRecipeDao.upsert(recipe.toSavedEntity())
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteSavedRecipe(id: RecipeId): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                savedRecipeDao.deleteById(id.raw)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeSavedRecipes(): Flow<List<Recipe>> =
        savedRecipeDao.observeAll()
            .distinctUntilChanged()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
}
