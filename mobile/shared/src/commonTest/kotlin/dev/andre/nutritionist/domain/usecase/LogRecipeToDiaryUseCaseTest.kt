package dev.andre.nutritionist.domain.usecase

import app.cash.turbine.test
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class LogRecipeToDiaryUseCaseTest {

    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var useCase: LogRecipeToDiaryUseCase

    @BeforeTest
    fun setUp() {
        diaryRepository = FakeDiaryRepository()
        dispatchers = TestCoroutineDispatchers()
        useCase = LogRecipeToDiaryUseCase(
            diaryRepository = diaryRepository,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `invoke calculates portion nutrition and cost and saves MealRecord with PENDING status`() = runTest {
        val recipe = Recipe(
            id = RecipeId("recipe-1"),
            title = "Куриная грудка с гречкой",
            mealType = MealType.LUNCH,
            totalWeightGrams = 400.0,
            nutrition = NutritionFacts(calories = 150.0, proteins = 18.0, fats = 3.0, carbs = 12.0),
            estimatedCost = Money.fromMinor(24000L), // 240.00 RUB for 400g -> 120.00 RUB for 200g
        )
        val recordedAt = Instant.fromEpochMilliseconds(1700005000000L)

        diaryRepository.observeDailyRecords(1700000000000L, 1700086400000L).test {
            assertTrue(awaitItem().isEmpty())

            val result = useCase(
                recipe = recipe,
                weightGrams = 200.0,
                mealType = MealType.DINNER,
                recordedAt = recordedAt,
            )

            assertTrue(result.isSuccess)
            val saved = result.getOrThrow()
            assertEquals(EntrySourceType.RECIPE, saved.sourceType)
            assertEquals("recipe-1", saved.sourceId)
            assertEquals("Куриная грудка с гречкой", saved.nameSnapshot)
            assertEquals(MealType.DINNER, saved.mealType)
            assertEquals(200.0, saved.weightGrams)
            assertEquals(NutritionFacts(calories = 300.0, proteins = 36.0, fats = 6.0, carbs = 24.0), saved.nutrition)
            assertEquals(Money.fromMinor(12000L), saved.cost)
            assertEquals(SyncStatus.PENDING, saved.syncStatus)
            assertTrue(saved.id.raw.isNotBlank())
            assertTrue(saved.idempotencyKey.raw.isNotBlank())

            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            assertEquals(saved.id, emitted.first().id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke derives nutrition from ingredients when recipe per100g is ZERO and keeps null cost when unknown`() = runTest {
        val recipe = Recipe(
            id = RecipeId("recipe-from-ing"),
            title = "Овсянка на воде",
            nutrition = NutritionFacts.ZERO,
            estimatedCost = Money.ZERO,
            ingredients = listOf(
                RecipeIngredient(
                    id = RecipeIngredientId("ing-oats"),
                    name = "Овсяные хлопья",
                    weightGrams = 100.0,
                    nutritionPer100g = NutritionFacts(calories = 350.0, proteins = 12.0, fats = 6.0, carbs = 60.0),
                ),
                RecipeIngredient(
                    id = RecipeIngredientId("ing-water"),
                    name = "Вода",
                    weightGrams = 200.0,
                    nutritionPer100g = NutritionFacts.ZERO,
                ),
            ),
        )

        val result = useCase(
            recipe = recipe,
            weightGrams = 150.0,
            recordedAt = Instant.fromEpochMilliseconds(1700005000000L),
        )

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        // 300g total -> per 100g: 116.67 kcal, 4.0 P, 2.0 F, 20.0 C -> 150g portion: 175.0 kcal, 6.0 P, 3.0 F, 30.0 C
        assertEquals(NutritionFacts(calories = 175.0, proteins = 6.0, fats = 3.0, carbs = 30.0), saved.nutrition)
        assertNull(saved.cost)
    }

    @Test
    fun `invoke returns ValidationError when recipe id is blank or weight is non-positive`() = runTest {
        val blankIdRecipe = Recipe(
            id = RecipeId("  "),
            title = "Без ID",
            nutrition = NutritionFacts(calories = 100.0),
        )
        val blankIdResult = useCase(blankIdRecipe, weightGrams = 100.0)
        assertTrue(blankIdResult.isFailure)
        val idError = assertIs<DomainError.ValidationError>(blankIdResult.exceptionOrNull())
        assertEquals("recipeId", idError.field)

        val validRecipe = Recipe(
            id = RecipeId("rec-valid"),
            title = "Валидный",
            nutrition = NutritionFacts(calories = 100.0),
        )
        val zeroWeightResult = useCase(validRecipe, weightGrams = 0.0)
        assertTrue(zeroWeightResult.isFailure)
        val weightError = assertIs<DomainError.ValidationError>(zeroWeightResult.exceptionOrNull())
        assertEquals("weightGrams", weightError.field)
        assertEquals(0, diaryRepository.addEntryCallCount)
    }

    @Test
    fun `invoke propagates failure from DiaryRepository`() = runTest {
        diaryRepository.shouldFailAddEntry = DomainError.NetworkUnavailable("Offline")
        val recipe = Recipe(
            id = RecipeId("rec-err"),
            title = "Ошибка",
            nutrition = NutritionFacts(calories = 100.0),
        )

        val result = useCase(recipe, weightGrams = 100.0)

        assertTrue(result.isFailure)
        assertIs<DomainError.NetworkUnavailable>(result.exceptionOrNull())
    }
}
