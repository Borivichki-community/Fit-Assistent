package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Асинхронный обработчик команд экрана AI-рецептов в архитектуре KoTEA MVI.
 */
class AiRecipeCommandsFlowHandler(
    private val recipeRepository: RecipeRepository,
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<AiRecipeCommand, AiRecipeEvent> {

    override fun handle(commands: Flow<AiRecipeCommand>): Flow<AiRecipeEvent> = flow {
        commands.collect { command ->
            when (command) {
                is AiRecipeCommand.Generate -> {
                    emit(
                        AiRecipeEvent.Internal.GenerationProgressUpdated(
                            phaseText = "Анализируем нутриенты и КБЖУ...",
                            progress = 0.35f,
                        ),
                    )
                    emit(
                        AiRecipeEvent.Internal.GenerationProgressUpdated(
                            phaseText = "Подбираем ингредиенты под бюджет...",
                            progress = 0.70f,
                        ),
                    )
                    emit(
                        AiRecipeEvent.Internal.GenerationProgressUpdated(
                            phaseText = "Шеф составляет пошаговый рецепт...",
                            progress = 0.95f,
                        ),
                    )
                    val result = withContext(dispatchers.io) {
                        recipeRepository.generateRecipe(command.prompt).fold(
                            onSuccess = { AiRecipeEvent.Internal.RecipeGenerated(it) },
                            onFailure = { AiRecipeEvent.Internal.GenerationFailed(it) },
                        )
                    }
                    emit(result)
                }

                is AiRecipeCommand.SaveRecipe -> {
                    val result = withContext(dispatchers.io) {
                        recipeRepository.saveRecipe(command.recipe).fold(
                            onSuccess = { AiRecipeEvent.Internal.RecipeSaved(it) },
                            onFailure = { AiRecipeEvent.Internal.GenerationFailed(it) },
                        )
                    }
                    emit(result)
                }

                is AiRecipeCommand.AddRecipeToDiary -> {
                    val recipe = command.recipe
                    val now = Instant.fromEpochMilliseconds(1732200000000L)
                    val entry = DiaryEntry(
                        id = DiaryEntryId("diary-gen-${recipe.id.raw}"),
                        userId = UserId("current_user"),
                        foodItemId = FoodItemId("ai-recipe-${recipe.id.raw}"),
                        foodName = recipe.title,
                        portionGrams = 300.0,
                        calories = recipe.calories,
                        proteins = recipe.proteins,
                        fats = recipe.fats,
                        carbs = recipe.carbs,
                        mealType = recipe.mealType,
                        date = LocalDate.fromEpochDays((now.toEpochMilliseconds() / 86_400_000L).toInt()),
                        recordedAt = now,
                        idempotencyKey = IdempotencyKey.generate(),
                        portionDescription = "1 порция (${recipe.calories} ккал)",
                    )
                    val result = withContext(dispatchers.io) {
                        diaryRepository.addEntry(entry).fold(
                            onSuccess = { AiRecipeEvent.Internal.RecipeAddedToDiary },
                            onFailure = { AiRecipeEvent.Internal.GenerationFailed(it) },
                        )
                    }
                    emit(result)
                }
            }
        }
    }
}
