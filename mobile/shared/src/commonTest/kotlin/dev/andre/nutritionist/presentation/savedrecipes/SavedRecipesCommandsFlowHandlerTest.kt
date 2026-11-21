package dev.andre.nutritionist.presentation.savedrecipes

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.AiRecipePrompt
import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.RecipeRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class SavedRecipesCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var recipeRepository: FakeRecipeRepository
    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var handler: SavedRecipesCommandsFlowHandler

    private val sampleRecipe = Recipe(
        id = RecipeId("rec-1"),
        title = "Куриная грудка с рисом",
        description = "Диетическое блюдо",
        mealType = MealType.LUNCH,
        calories = 480,
        proteins = 40.0,
        fats = 10.0,
        carbs = 50.0,
        estimatedCostRub = 180,
        cookingTimeMinutes = 20,
        ingredients = listOf(RecipeIngredient("Куриное филе", "150 г")),
        steps = listOf("Отварить грудку"),
        isFavorite = false,
    )

    @BeforeTest
    fun setUp() {
        recipeRepository = FakeRecipeRepository()
        diaryRepository = FakeDiaryRepository()
        handler = SavedRecipesCommandsFlowHandler(
            recipeRepository = recipeRepository,
            diaryRepository = diaryRepository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun observeSavedRecipes_emits_RecipesLoaded_on_success() = runTest(testDispatcher) {
        recipeRepository.savedRecipesResult = Result.success(listOf(sampleRecipe))

        handler.handle(flowOf(SavedRecipesCommand.ObserveSavedRecipes)).test {
            val event = awaitItem()
            val loaded = assertIs<SavedRecipesEvent.Internal.RecipesLoaded>(event)
            assertEquals(1, loaded.recipes.size)
            assertEquals(sampleRecipe, loaded.recipes.first())
            awaitComplete()
        }
    }

    @Test
    fun observeSavedRecipes_emits_RecipesLoadFailed_on_error() = runTest(testDispatcher) {
        val error = RuntimeException("Failed load")
        recipeRepository.savedRecipesResult = Result.failure(error)

        handler.handle(flowOf(SavedRecipesCommand.ObserveSavedRecipes)).test {
            val event = awaitItem()
            val failed = assertIs<SavedRecipesEvent.Internal.RecipesLoadFailed>(event)
            assertEquals(error, failed.throwable)
            awaitComplete()
        }
    }

    @Test
    fun toggleFavorite_emits_RecipeToggled_and_updated_RecipesLoaded() = runTest(testDispatcher) {
        val updatedRecipe = sampleRecipe.copy(isFavorite = true)
        recipeRepository.toggleFavoriteResult = Result.success(updatedRecipe)
        recipeRepository.savedRecipesResult = Result.success(listOf(updatedRecipe))

        handler.handle(flowOf(SavedRecipesCommand.ToggleFavorite(sampleRecipe.id))).test {
            val toggledEvent = awaitItem()
            val toggled = assertIs<SavedRecipesEvent.Internal.RecipeToggled>(toggledEvent)
            assertEquals(updatedRecipe, toggled.recipe)

            val loadedEvent = awaitItem()
            val loaded = assertIs<SavedRecipesEvent.Internal.RecipesLoaded>(loadedEvent)
            assertEquals(listOf(updatedRecipe), loaded.recipes)
            awaitComplete()
        }
    }

    @Test
    fun addRecipeToDiary_emits_RecipeAddedToDiary_on_success() = runTest(testDispatcher) {
        handler.handle(flowOf(SavedRecipesCommand.AddRecipeToDiary(sampleRecipe))).test {
            val event = awaitItem()
            val added = assertIs<SavedRecipesEvent.Internal.RecipeAddedToDiary>(event)
            assertEquals(sampleRecipe.id, added.recipeId)
            awaitComplete()
        }
    }

    private class FakeRecipeRepository : RecipeRepository {
        var generateResult: Result<Recipe> = Result.failure(NotImplementedError())
        var savedRecipesResult: Result<List<Recipe>> = Result.success(emptyList())
        var toggleFavoriteResult: Result<Recipe> = Result.failure(NotImplementedError())
        var saveRecipeResult: Result<Recipe> = Result.failure(NotImplementedError())

        override suspend fun generateRecipe(prompt: AiRecipePrompt): Result<Recipe> = generateResult
        override suspend fun getSavedRecipes(): Result<List<Recipe>> = savedRecipesResult
        override fun observeSavedRecipes(): Flow<List<Recipe>> = emptyFlow()
        override suspend fun toggleFavorite(recipeId: RecipeId): Result<Recipe> = toggleFavoriteResult
        override suspend fun saveRecipe(recipe: Recipe): Result<Recipe> = saveRecipeResult
    }

    private class FakeDiaryRepository : DiaryRepository {
        val addedEntries = mutableListOf<DiaryEntry>()
        var errorToThrow: Throwable? = null

        override suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary> =
            Result.success(DailyDiary(userId, date, addedEntries))

        override fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary> = emptyFlow()

        override suspend fun addEntry(entry: DiaryEntry): Result<DiaryEntry> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            addedEntries.add(entry)
            return Result.success(entry)
        }

        override suspend fun deleteEntry(id: DiaryEntryId): Result<Unit> = Result.success(Unit)
    }
}
