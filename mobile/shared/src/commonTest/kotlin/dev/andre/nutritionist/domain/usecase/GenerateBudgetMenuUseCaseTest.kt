package dev.andre.nutritionist.domain.usecase

import app.cash.turbine.test
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.MenuSource
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.fake.FakeCatalogRepository
import dev.andre.nutritionist.fake.FakeRecipeRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GenerateBudgetMenuUseCaseTest {

    private lateinit var recipeRepository: FakeRecipeRepository
    private lateinit var catalogRepository: FakeCatalogRepository
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var useCase: GenerateBudgetMenuUseCase

    @BeforeTest
    fun setUp() {
        recipeRepository = FakeRecipeRepository()
        catalogRepository = FakeCatalogRepository()
        dispatchers = TestCoroutineDispatchers()
        useCase = GenerateBudgetMenuUseCase(
            recipeRepository = recipeRepository,
            catalogRepository = catalogRepository,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `invoke returns failure when TargetPeriod is UNSPECIFIED`() = runTest {
        val params = GenerateBudgetMenuParams(
            period = TargetPeriod.UNSPECIFIED,
            targetCalories = 2000.0,
            maxBudget = Money.fromRubles(1000.0),
        )

        val result = useCase(params)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<DomainError.ValidationError>(error)
        assertEquals("period", error.field)
    }

    @Test
    fun `invoke returns failure when budget is zero or negative`() = runTest {
        val paramsZero = GenerateBudgetMenuParams(
            period = TargetPeriod.DAY,
            targetCalories = 2000.0,
            maxBudget = Money.ZERO,
        )
        val resultZero = useCase(paramsZero)
        assertTrue(resultZero.isFailure)
        assertIs<DomainError.InvalidBudgetError>(resultZero.exceptionOrNull())

        val paramsNegative = GenerateBudgetMenuParams(
            period = TargetPeriod.DAY,
            targetCalories = 2000.0,
            maxBudget = Money.fromMinor(-500L),
        )
        val resultNegative = useCase(paramsNegative)
        assertTrue(resultNegative.isFailure)
        assertIs<DomainError.InvalidBudgetError>(resultNegative.exceptionOrNull())
    }

    @Test
    fun `invoke returns failure when calories is zero or negative`() = runTest {
        val paramsZero = GenerateBudgetMenuParams(
            period = TargetPeriod.DAY,
            targetCalories = 0.0,
            maxBudget = Money.fromRubles(500.0),
        )
        val resultZero = useCase(paramsZero)
        assertTrue(resultZero.isFailure)
        assertIs<DomainError.InvalidBudgetError>(resultZero.exceptionOrNull())

        val paramsNegative = GenerateBudgetMenuParams(
            period = TargetPeriod.DINNER,
            targetCalories = -200.0,
            maxBudget = Money.fromRubles(500.0),
        )
        val resultNegative = useCase(paramsNegative)
        assertTrue(resultNegative.isFailure)
        assertIs<DomainError.InvalidBudgetError>(resultNegative.exceptionOrNull())
    }

    @Test
    fun `invoke returns failure when preferred products cost exceeds budget`() = runTest {
        val prod1 = ProductId("prod-chicken")
        val prod2 = ProductId("prod-beef")

        catalogRepository.averagePrices[prod1] = Money.fromRubles(350.0) // 35000 minor
        catalogRepository.averagePrices[prod2] = Money.fromRubles(500.0) // 50000 minor
        // Total = 850.00 RUB (85000 minor), but budget is 500.00 RUB (50000 minor)

        val params = GenerateBudgetMenuParams(
            period = TargetPeriod.DAY,
            targetCalories = 2200.0,
            maxBudget = Money.fromRubles(500.0),
            preferredProductIds = listOf(prod1, prod2),
        )

        val result = useCase(params)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<DomainError.BudgetExceededError>(error)
        assertEquals(85000L, error.requiredMinor)
        assertEquals(50000L, error.maximumMinor)
    }

    @Test
    fun `invoke succeeds and emits Initializing then Completed for valid params`() = runTest {
        val testPlan = MealPlan(
            id = "plan-123",
            period = TargetPeriod.DINNER,
            meals = listOf(
                Recipe(
                    id = RecipeId("rec-1"),
                    title = "Куриное филе на пару с рисом",
                    mealType = MealType.DINNER,
                    nutrition = NutritionFacts(calories = 626.5, proteins = 66.0, fats = 8.5, carbs = 70.0),
                    estimatedCost = Money.fromRubles(105.0),
                    cookingTimeMinutes = 25,
                ),
            ),
            totalNutrition = NutritionFacts(calories = 626.5, proteins = 66.0, fats = 8.5, carbs = 70.0),
            estimatedCost = Money.fromRubles(105.0),
            cookingInstructions = "1. Отварить рис. 2. Приготовить филе на пару.",
            source = MenuSource.GENERATED,
        )
        recipeRepository.planToReturn = testPlan

        val params = GenerateBudgetMenuParams(
            period = TargetPeriod.DINNER,
            targetCalories = 650.0,
            maxBudget = Money.fromRubles(300.0),
        )

        val result = useCase(params)
        assertTrue(result.isSuccess)

        val progressFlow = result.getOrThrow()
        progressFlow.test {
            val first = awaitItem()
            assertIs<MenuGenerationProgress.Initializing>(first)

            val second = awaitItem()
            assertIs<MenuGenerationProgress.Completed>(second)
            assertEquals("plan-123", second.plan.id)
            assertEquals(1, second.plan.meals.size)
            assertEquals("Куриное филе на пару с рисом", second.plan.meals.first().title)

            awaitComplete()
        }
        assertEquals(1, recipeRepository.generateCallCount)
    }
}
