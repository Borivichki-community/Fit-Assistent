package dev.andre.nutritionist.presentation.savedrecipes

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
 * Асинхронный обработчик команд экрана сохранённых рецептов.
 */
class SavedRecipesCommandsFlowHandler(
    private val recipeRepository: RecipeRepository,
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<SavedRecipesCommand, SavedRecipesEvent> {

    override fun handle(commands: Flow<SavedRecipesCommand>): Flow<SavedRecipesEvent> = flow {
        commands.collect { command ->
            when (command) {
                is SavedRecipesCommand.ObserveSavedRecipes -> {
                    val result = withContext(dispatchers.io) {
                        recipeRepository.getSavedRecipes().fold(
                            onSuccess = { SavedRecipesEvent.Internal.RecipesLoaded(it) },
                            onFailure = { SavedRecipesEvent.Internal.RecipesLoadFailed(it) },
                        )
                    }
                    emit(result)
                }

                is SavedRecipesCommand.ToggleFavorite -> {
                    val result = withContext(dispatchers.io) {
                        recipeRepository.toggleFavorite(command.recipeId).fold(
                            onSuccess = { SavedRecipesEvent.Internal.RecipeToggled(it) },
                            onFailure = { SavedRecipesEvent.Internal.ToggleFailed(it) },
                        )
                    }
                    emit(result)
                    val updatedListResult = withContext(dispatchers.io) {
                        recipeRepository.getSavedRecipes().fold(
                            onSuccess = { SavedRecipesEvent.Internal.RecipesLoaded(it) },
                            onFailure = { SavedRecipesEvent.Internal.RecipesLoadFailed(it) },
                        )
                    }
                    emit(updatedListResult)
                }

                is SavedRecipesCommand.AddRecipeToDiary -> {
                    val recipe = command.recipe
                    val now = Instant.fromEpochMilliseconds(1732200000000L)
                    val entry = DiaryEntry(
                        id = DiaryEntryId("diary-saved-${recipe.id.raw}"),
                        userId = UserId("current_user"),
                        foodItemId = FoodItemId("saved-recipe-${recipe.id.raw}"),
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
                            onSuccess = { SavedRecipesEvent.Internal.RecipeAddedToDiary(recipe.id) },
                            onFailure = { SavedRecipesEvent.Internal.AddToDiaryFailed(it) },
                        )
                    }
                    emit(result)
                }
            }
        }
    }
}
