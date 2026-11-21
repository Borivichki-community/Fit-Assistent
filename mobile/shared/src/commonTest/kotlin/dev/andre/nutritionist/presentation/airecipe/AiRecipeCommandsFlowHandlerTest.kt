package dev.andre.nutritionist.presentation.airecipe

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

class AiRecipeCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var recipeRepository: FakeRecipeRepository
    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var handler: AiRecipeCommandsFlowHandler

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
        handler = AiRecipeCommandsFlowHandler(
            recipeRepository = recipeRepository,
            diaryRepository = diaryRepository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun generate_command_emits_progress_updates_and_RecipeGenerated_on_success() = runTest(testDispatcher) {
        recipeRepository.recipeToReturn = sampleRecipe

        handler.handle(flowOf(AiRecipeCommand.Generate(AiRecipePrompt()))).test {
            val p1 = awaitItem()
            assertIs<AiRecipeEvent.Internal.GenerationProgressUpdated>(p1)
            val p2 = awaitItem()
            assertIs<AiRecipeEvent.Internal.GenerationProgressUpdated>(p2)
            val p3 = awaitItem()
            assertIs<AiRecipeEvent.Internal.GenerationProgressUpdated>(p3)

            val success = awaitItem()
            assertIs<AiRecipeEvent.Internal.RecipeGenerated>(success)
            assertEquals(sampleRecipe, success.recipe)
            awaitComplete()
        }
    }

    @Test
    fun generate_command_emits_GenerationFailed_on_repository_error() = runTest(testDispatcher) {
        recipeRepository.errorToThrow = RuntimeException("AI API failure")

        handler.handle(flowOf(AiRecipeCommand.Generate(AiRecipePrompt()))).test {
            awaitItem()
            awaitItem()
            awaitItem()
            val failed = awaitItem()
            assertIs<AiRecipeEvent.Internal.GenerationFailed>(failed)
            assertEquals("AI API failure", failed.error.message)
            awaitComplete()
        }
    }

    @Test
    fun saveRecipe_command_emits_RecipeSaved_on_repository_success() = runTest(testDispatcher) {
        recipeRepository.recipeToReturn = sampleRecipe

        handler.handle(flowOf(AiRecipeCommand.SaveRecipe(sampleRecipe))).test {
            val saved = awaitItem()
            assertIs<AiRecipeEvent.Internal.RecipeSaved>(saved)
            assertEquals(sampleRecipe, saved.recipe)
            awaitComplete()
        }
    }

    @Test
    fun addRecipeToDiary_command_emits_RecipeAddedToDiary_on_diary_repository_success() = runTest(testDispatcher) {
        handler.handle(flowOf(AiRecipeCommand.AddRecipeToDiary(sampleRecipe))).test {
            val added = awaitItem()
            assertIs<AiRecipeEvent.Internal.RecipeAddedToDiary>(added)
            assertEquals(1, diaryRepository.addedEntries.size)
            assertEquals(sampleRecipe.title, diaryRepository.addedEntries.first().foodName)
            awaitComplete()
        }
    }

    private class FakeRecipeRepository : RecipeRepository {
        var recipeToReturn: Recipe? = null
        var errorToThrow: Throwable? = null

        override suspend fun generateRecipe(prompt: AiRecipePrompt): Result<Recipe> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(requireNotNull(recipeToReturn))
        }

        override suspend fun getSavedRecipes(): Result<List<Recipe>> =
            Result.success(recipeToReturn?.let { listOf(it) } ?: emptyList())

        override fun observeSavedRecipes(): Flow<List<Recipe>> = emptyFlow()

        override suspend fun toggleFavorite(recipeId: RecipeId): Result<Recipe> =
            Result.success(requireNotNull(recipeToReturn))

        override suspend fun saveRecipe(recipe: Recipe): Result<Recipe> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(recipe)
        }
    }

    private class FakeDiaryRepository : DiaryRepository {
        val addedEntries = mutableListOf<DiaryEntry>()

        override suspend fun getDailyDiary(userId: UserId, date: LocalDate): Result<DailyDiary> =
            Result.success(DailyDiary(userId, date, addedEntries))

        override fun observeDailyDiary(userId: UserId, date: LocalDate): Flow<DailyDiary> = emptyFlow()

        override suspend fun addEntry(entry: DiaryEntry): Result<DiaryEntry> {
            addedEntries.add(entry)
            return Result.success(entry)
        }

        override suspend fun deleteEntry(id: DiaryEntryId): Result<Unit> = Result.success(Unit)
    }
}
