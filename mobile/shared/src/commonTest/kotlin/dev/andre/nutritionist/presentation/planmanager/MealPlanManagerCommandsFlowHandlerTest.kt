package dev.andre.nutritionist.presentation.planmanager

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MealPlanManagerCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var handler: MealPlanManagerCommandsFlowHandler

    private val samplePlan = MealPlan(
        id = MealPlanId("plan-1"),
        title = "Баланс",
        description = "Описание",
        category = "Баланс",
        targetCalories = 2000,
        proteinsGrams = 120.0,
        fatsGrams = 70.0,
        carbsGrams = 220.0,
        estimatedWeeklyBudgetRub = 3500.0,
    )

    private val sampleDays = listOf(
        MealPlanDay(
            dayNumber = 1,
            dayName = "Понедельник",
            shortDayName = "Пн",
            targetCalories = 2000,
            meals = emptyList(),
        ),
    )

    private val sampleGroceries = listOf(
        GroceryItem(
            id = GroceryItemId("g-1"),
            name = "Яблоки",
            amount = "1 кг",
            category = "Фрукты",
            isBought = false,
            estimatedPriceRub = 150.0,
        ),
    )

    @BeforeTest
    fun setUp() {
        repository = FakeMealPlanRepository()
        handler = MealPlanManagerCommandsFlowHandler(
            mealPlanRepository = repository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun `Load command emits Loaded with plan, days and groceries on success`() = runTest(testDispatcher) {
        repository.activePlanToReturn = samplePlan
        repository.daysToReturn = sampleDays
        repository.groceriesToReturn = sampleGroceries

        handler.handle(flowOf(MealPlanManagerCommand.Load(UserId("user-1")))).test {
            val event = awaitItem()
            assertIs<MealPlanManagerEvent.Internal.Loaded>(event)
            assertEquals(samplePlan, event.activePlan)
            assertEquals(sampleDays, event.days)
            assertEquals(sampleGroceries, event.groceryList)
            awaitComplete()
        }
    }

    @Test
    fun `Load command emits Loaded with null when no active plan`() = runTest(testDispatcher) {
        repository.activePlanToReturn = null

        handler.handle(flowOf(MealPlanManagerCommand.Load(UserId("user-1")))).test {
            val event = awaitItem()
            assertIs<MealPlanManagerEvent.Internal.Loaded>(event)
            assertEquals(null, event.activePlan)
            awaitComplete()
        }
    }

    @Test
    fun `ToggleGrocery command emits GroceryToggled on success`() = runTest(testDispatcher) {
        repository.toggleBoughtResult = true

        handler.handle(
            flowOf(
                MealPlanManagerCommand.ToggleGrocery(
                    planId = samplePlan.id,
                    itemId = GroceryItemId("g-1"),
                ),
            ),
        ).test {
            val event = awaitItem()
            assertIs<MealPlanManagerEvent.Internal.GroceryToggled>(event)
            assertEquals(GroceryItemId("g-1"), event.itemId)
            assertEquals(true, event.isBought)
            awaitComplete()
        }
    }

    @Test
    fun `DeactivatePlan command emits PlanDeactivated on success`() = runTest(testDispatcher) {
        handler.handle(flowOf(MealPlanManagerCommand.DeactivatePlan(UserId("user-1")))).test {
            val event = awaitItem()
            assertIs<MealPlanManagerEvent.Internal.PlanDeactivated>(event)
            awaitComplete()
        }
    }

    @Test
    fun `Load command emits Failed when repository throws`() = runTest(testDispatcher) {
        repository.errorToThrow = RuntimeException("DB error")

        handler.handle(flowOf(MealPlanManagerCommand.Load(UserId("user-1")))).test {
            val event = awaitItem()
            assertIs<MealPlanManagerEvent.Internal.Failed>(event)
            assertEquals("DB error", event.error.message)
            awaitComplete()
        }
    }

    private class FakeMealPlanRepository : MealPlanRepository {
        var activePlanToReturn: MealPlan? = null
        var daysToReturn: List<MealPlanDay> = emptyList()
        var groceriesToReturn: List<GroceryItem> = emptyList()
        var toggleBoughtResult: Boolean = true
        var errorToThrow: Throwable? = null

        override suspend fun getMealPlans(query: String, category: String): Result<List<MealPlan>> =
            Result.success(emptyList())

        override suspend fun getMealPlanById(id: MealPlanId): Result<MealPlan?> =
            Result.success(activePlanToReturn)

        override suspend fun toggleFavorite(id: MealPlanId): Result<Boolean> =
            Result.success(true)

        override suspend fun getActiveMealPlan(userId: UserId): Result<MealPlan?> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(activePlanToReturn)
        }

        override suspend fun setActiveMealPlan(userId: UserId, planId: MealPlanId): Result<Unit> =
            Result.success(Unit)

        override suspend fun deactivateMealPlan(userId: UserId): Result<Unit> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(Unit)
        }

        override suspend fun getMealPlanDays(planId: MealPlanId): Result<List<MealPlanDay>> =
            Result.success(daysToReturn)

        override suspend fun getGroceryList(planId: MealPlanId): Result<List<GroceryItem>> =
            Result.success(groceriesToReturn)

        override suspend fun toggleGroceryItem(planId: MealPlanId, itemId: GroceryItemId): Result<Boolean> {
            val err = errorToThrow
            if (err != null) return Result.failure(err)
            return Result.success(toggleBoughtResult)
        }
    }
}
