package dev.andre.nutritionist.domain.usecase

import dev.andre.nutritionist.domain.calculator.RecipeNutritionCalculator
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.repository.DiaryRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

class LogRecipeToDiaryUseCase(
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers(),
) {

    suspend operator fun invoke(
        recipe: Recipe,
        weightGrams: Double,
        mealType: MealType = recipe.mealType,
        recordedAt: Instant = Clock.System.now(),
    ): Result<MealRecord> = withContext(dispatchers.io) {
        if (recipe.id.raw.isBlank()) {
            return@withContext Result.failure(
                DomainError.ValidationError(field = "recipeId", reason = "Recipe ID must not be blank"),
            )
        }
        if (weightGrams <= 0.0 || weightGrams.isNaN() || weightGrams.isInfinite()) {
            return@withContext Result.failure(
                DomainError.ValidationError(field = "weightGrams", reason = "Weight must be greater than zero"),
            )
        }

        val per100g = if (recipe.nutrition != NutritionFacts.ZERO) {
            recipe.nutrition
        } else {
            RecipeNutritionCalculator.calculatePer100g(recipe.ingredients)
        }

        val portionNutrition = RecipeNutritionCalculator.calculatePortion(
            nutritionPer100g = per100g,
            weightGrams = weightGrams,
        )

        val totalRecipeWeight = if (recipe.totalWeightGrams > 0.0) {
            recipe.totalWeightGrams
        } else {
            RecipeNutritionCalculator.calculateSummary(recipe.ingredients).totalWeightGrams
        }

        val portionCost = RecipeNutritionCalculator.calculatePortionCost(
            totalCost = recipe.estimatedCost,
            totalWeightGrams = totalRecipeWeight,
            portionWeightGrams = weightGrams,
        )

        val record = MealRecord(
            id = MealRecordId.generate(),
            mealType = mealType,
            recordedAt = recordedAt,
            sourceType = EntrySourceType.RECIPE,
            sourceId = recipe.id.raw,
            nameSnapshot = recipe.title,
            weightGrams = weightGrams,
            nutrition = portionNutrition,
            cost = portionCost,
            idempotencyKey = IdempotencyKey.generate(),
            syncStatus = SyncStatus.PENDING,
        )

        try {
            diaryRepository.addEntry(record)
        } catch (e: CancellationException) {
            throw e
        }
    }
}
