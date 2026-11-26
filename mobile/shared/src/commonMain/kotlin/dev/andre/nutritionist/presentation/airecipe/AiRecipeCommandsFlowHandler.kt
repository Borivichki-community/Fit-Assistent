package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuUseCase
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.LogRecipeToDiaryUseCase
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import dev.andre.nutritionist.domain.repository.CatalogRepository

class AiRecipeCommandsFlowHandler(
    private val generate: GenerateBudgetMenuUseCase,
    private val catalog: CatalogRepository,
    private val logRecipe: LogRecipeToDiaryUseCase,
) : CommandsFlowHandler<AiRecipeCommand, AiRecipeEvent> {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun handle(commands: Flow<AiRecipeCommand>): Flow<AiRecipeEvent> =
        commands.transformLatest { command ->
            when (command) {
                AiRecipeCommand.Cancel -> Unit
                is AiRecipeCommand.Generate -> {
                    val prompt = command.prompt
                    val nutrition = prompt.nutritionTarget
                    val budget = if (prompt.targetBudgetRub == AiRecipePrompt.NO_BUDGET_LIMIT) {
                        AiRecipePrompt.MAX_SUPPORTED_BUDGET_RUB
                    } else {
                        prompt.targetBudgetRub
                    }
                    val params = GenerateBudgetMenuParams(
                        period = when (prompt.mealType) {
                            MealType.BREAKFAST -> TargetPeriod.BREAKFAST
                            MealType.LUNCH -> TargetPeriod.LUNCH
                            MealType.DINNER -> TargetPeriod.DINNER
                            MealType.SNACK -> TargetPeriod.SNACK
                        },
                        targetCalories = nutrition.calories,
                        targetProteinsGrams = nutrition.proteins,
                        targetFatsGrams = nutrition.fats,
                        targetCarbsGrams = nutrition.carbs,
                        maxBudget = Money.fromRubles(budget.toDouble()),
                        userNote = listOf(prompt.query, prompt.dietaryPreference).filter(String::isNotBlank).joinToString(" "),
                    )
                    flow {
                        emitAll(generate(params).getOrThrow())
                    }.map { progress ->
                        when (progress) {
                            MenuGenerationProgress.Initializing -> AiRecipeEvent.Internal.ProgressUpdated("Анализируем предпочтения и норму КБЖУ…", 0.25f)
                            is MenuGenerationProgress.StreamingDay -> AiRecipeEvent.Internal.ProgressUpdated("Подбираем ингредиенты по каталогу цен…", 0.65f)
                            is MenuGenerationProgress.Completed -> AiRecipeEvent.Internal.RecipeGenerated(
                                withIngredientPrices(checkNotNull(progress.plan.meals.firstOrNull()) { "Сервис вернул пустой план питания" }).let { it.copy(instructions = it.instructions ?: progress.plan.cookingInstructions) },
                            )
                        }
                    }.catch { emit(AiRecipeEvent.Internal.GenerationFailed(it)) }.collect { emit(it) }
                }
                is AiRecipeCommand.AddRecipeToDiary -> {
                    val recipe = command.recipe
                    val weight = recipe.totalWeightGrams.takeIf { it > 0 } ?: recipe.ingredients.sumOf { it.weightGrams }
                    val record = try {
                        logRecipe(recipe, weight / recipe.servingsCount.coerceAtLeast(1), command.mealType, Instant.fromEpochMilliseconds(command.dateEpochMs)).getOrThrow()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        emit(AiRecipeEvent.Internal.SaveFailed(failure))
                        return@transformLatest
                    }
                    emit(AiRecipeEvent.Internal.RecipeAddedToDiary(record))
                }
            }
        }
    private suspend fun withIngredientPrices(recipe: dev.andre.nutritionist.domain.model.Recipe): dev.andre.nutritionist.domain.model.Recipe {
        val ids = recipe.ingredients.mapNotNull { it.productId }.distinct()
        if (ids.isEmpty()) return recipe
        val prices = catalog.getAveragePrices(ids).getOrElse {
            if (it is CancellationException) throw it
            emptyMap()
        }
        return recipe.copy(ingredients = recipe.ingredients.map { ingredient ->
            val price = ingredient.productId?.let(prices::get)
            ingredient.copy(cost = ingredient.cost ?: price?.times(ingredient.weightGrams / 1000.0))
        })
    }
}
