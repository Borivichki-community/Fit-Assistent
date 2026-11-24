package dev.andre.nutritionist.domain.usecase

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.repository.CatalogRepository
import dev.andre.nutritionist.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed interface MenuGenerationProgress {
    data object Initializing : MenuGenerationProgress
    data class StreamingDay(val dayNumber: Int, val meal: MealPlan) : MenuGenerationProgress
    data class Completed(val plan: MealPlan) : MenuGenerationProgress
}

data class GenerateBudgetMenuParams(
    val period: TargetPeriod,
    val targetCalories: Double,
    val maxBudget: Money,
    val targetProteinsGrams: Double = 0.0,
    val targetFatsGrams: Double = 0.0,
    val targetCarbsGrams: Double = 0.0,
    val regionId: RegionId? = null,
    val preferredProductIds: List<ProductId> = emptyList(),
    val excludedProductIds: List<ProductId> = emptyList(),
    val userNote: String = "",
)

class GenerateBudgetMenuUseCase(
    private val recipeRepository: RecipeRepository,
    private val catalogRepository: CatalogRepository,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend operator fun invoke(params: GenerateBudgetMenuParams): Result<Flow<MenuGenerationProgress>> =
        withContext(dispatchers.io) {
            // Fail-Fast: TargetPeriod validation
            if (params.period == TargetPeriod.UNSPECIFIED) {
                return@withContext Result.failure(
                    DomainError.ValidationError(field = "period", reason = "Target period must be specified"),
                )
            }

            // Fail-Fast: Budget and calorie validation
            if (params.maxBudget.amountMinor <= 0L) {
                return@withContext Result.failure(
                    DomainError.InvalidBudgetError("Budget must be greater than zero, got ${params.maxBudget.amountMinor} minor units"),
                )
            }
            if (params.targetCalories <= 0.0) {
                return@withContext Result.failure(
                    DomainError.InvalidBudgetError("Target calories must be greater than zero, got ${params.targetCalories}"),
                )
            }

            // Optional coordination with CatalogRepository for preferred products budget validation
            if (params.preferredProductIds.isNotEmpty()) {
                val pricesResult = catalogRepository.getAveragePrices(
                    productIds = params.preferredProductIds,
                    regionId = params.regionId,
                )
                pricesResult.onSuccess { pricesMap ->
                    val totalPreferredCostMinor = pricesMap.values.sumOf { it.amountMinor }
                    if (totalPreferredCostMinor > params.maxBudget.amountMinor) {
                        return@withContext Result.failure(
                            DomainError.BudgetExceededError(
                                requiredMinor = totalPreferredCostMinor,
                                maximumMinor = params.maxBudget.amountMinor,
                            ),
                        )
                    }
                }
            }

            val progressFlow: Flow<MenuGenerationProgress> = flow {
                emit(MenuGenerationProgress.Initializing)

                when (params.period) {
                    TargetPeriod.DAY,
                    TargetPeriod.BREAKFAST,
                    TargetPeriod.LUNCH,
                    TargetPeriod.DINNER,
                    TargetPeriod.SNACK,
                    -> {
                        val planResult = recipeRepository.generateMealPlan(params)
                        val plan = planResult.getOrThrow()
                        emit(MenuGenerationProgress.Completed(plan))
                    }
                    TargetPeriod.UNSPECIFIED -> {
                        // Handled above in fail-fast check
                    }
                }
            }.flowOn(dispatchers.io)

            Result.success(progressFlow)
        }
}
