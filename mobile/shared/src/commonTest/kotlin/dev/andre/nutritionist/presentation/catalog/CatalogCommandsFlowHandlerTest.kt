package dev.andre.nutritionist.presentation.catalog

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class CatalogCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var handler: CatalogCommandsFlowHandler

    private val samplePlan = MealPlan(
        id = MealPlanId("plan-1"),
        title = "Средиземноморский баланс",
        description = "Рыба и овощи",
        category = "Баланс",
        targetCalories = 2200,
        proteinsGrams = 130.0,
        fatsGrams = 75.0,
        carbsGrams = 250.0,
        estimatedWeeklyBudgetRub = 4500.0,
        isFavorite = false,
    )

    @BeforeTest
    fun setUp() {
        repository = FakeMealPlanRepository()
        handler = CatalogCommandsFlowHandler(
            mealPlanRepository = repository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun `LoadPlans command emits PlansLoaded on repository success`() = runTest(testDispatcher) {
        repository.plansToReturn = listOf(samplePlan)

        handler.handle(flowOf(CatalogCommand.LoadPlans("", "Все"))).test {
            val event = awaitItem()
            assertIs<CatalogEvent.Internal.PlansLoaded>(event)
            assertEquals(listOf(samplePlan), event.plans)
            awaitComplete()
        }
    }

    @Test
    fun `LoadPlans command emits LoadFailed on repository error`() = runTest(testDispatcher) {
        repository.errorToThrow = RuntimeException("Catalog network error")

        handler.handle(flowOf(CatalogCommand.LoadPlans("", "Все"))).test {
            val event = awaitItem()
            assertIs<CatalogEvent.Internal.LoadFailed>(event)
            assertEquals("Catalog network error", event.error.message)
            awaitComplete()
        }
    }

    @Test
    fun `ToggleFavorite command emits FavoriteUpdated on repository success`() = runTest(testDispatcher) {
        repository.isFavoriteToReturn = true

        handler.handle(flowOf(CatalogCommand.ToggleFavorite(samplePlan.id))).test {
            val event = awaitItem()
            assertIs<CatalogEvent.Internal.FavoriteUpdated>(event)
            assertEquals(samplePlan.id, event.planId)
            assertEquals(true, event.isFavorite)
            awaitComplete()
        }
    }

    private class FakeMealPlanRepository : MealPlanRepository {
        var plansToReturn: List<MealPlan> = emptyList()
        var isFavoriteToReturn: Boolean = false
        var errorToThrow: Throwable? = null

        override suspend fun getMealPlans(query: String, category: String): Result<List<MealPlan>> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(plansToReturn)
        }

        override suspend fun getMealPlanById(id: MealPlanId): Result<MealPlan?> =
            Result.success(plansToReturn.find { it.id == id })

        override suspend fun toggleFavorite(id: MealPlanId): Result<Boolean> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(isFavoriteToReturn)
        }
    }
}
