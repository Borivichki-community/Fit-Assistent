package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.domain.model.GroceryItem
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.domain.model.MealPlanMealItem
import dev.andre.nutritionist.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MealPlanManagerUpdateTest {

    private val update = MealPlanManagerUpdate()

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
            meals = listOf(
                MealPlanMealItem(
                    title = "Овсянка",
                    mealType = "Завтрак",
                    calories = 400,
                    proteinsGrams = 12.0,
                    fatsGrams = 8.0,
                    carbsGrams = 60.0,
                ),
            ),
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

    @Test
    fun `Ui Load transitions to Loading and issues Load command`() {
        val initialState = MealPlanManagerState()
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.Load(UserId("user-1")),
        )

        val state = requireNotNull(next.state)
        assertIs<MealPlanManagerState.Content.Loading>(state.content)
        assertEquals(UserId("user-1"), state.userId)
        assertEquals(1, next.commands.size)
        assertEquals(MealPlanManagerCommand.Load(UserId("user-1")), next.commands.first())
        assertTrue(next.news.isEmpty())
    }

    @Test
    fun `Ui Retry re-issues Load command with remembered userId`() {
        val initialState = MealPlanManagerState(userId = UserId("user-custom"))
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.Retry,
        )

        val state = requireNotNull(next.state)
        assertIs<MealPlanManagerState.Content.Loading>(state.content)
        assertEquals(1, next.commands.size)
        assertEquals(MealPlanManagerCommand.Load(UserId("user-custom")), next.commands.first())
    }

    @Test
    fun `Ui SelectTab switches active tab`() {
        val successContent = MealPlanManagerState.Content.Success(
            activePlan = samplePlan,
            days = sampleDays,
            groceryList = sampleGroceries,
            selectedTab = PlanManagerTab.SCHEDULE,
        )
        val initialState = MealPlanManagerState(
            content = successContent,
            selectedTab = PlanManagerTab.SCHEDULE,
        )

        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.SelectTab(PlanManagerTab.GROCERY_LIST),
        )

        val state = requireNotNull(next.state)
        assertEquals(PlanManagerTab.GROCERY_LIST, state.selectedTab)
        val content = state.content
        assertIs<MealPlanManagerState.Content.Success>(content)
        assertEquals(PlanManagerTab.GROCERY_LIST, content.selectedTab)
    }

    @Test
    fun `Ui SelectDay updates selected day index`() {
        val successContent = MealPlanManagerState.Content.Success(
            activePlan = samplePlan,
            days = sampleDays,
            groceryList = sampleGroceries,
            selectedDayIndex = 0,
        )
        val initialState = MealPlanManagerState(content = successContent)

        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.SelectDay(3),
        )

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<MealPlanManagerState.Content.Success>(content)
        assertEquals(3, content.selectedDayIndex)
    }

    @Test
    fun `Ui ToggleGrocery issues ToggleGrocery command with current planId`() {
        val successContent = MealPlanManagerState.Content.Success(
            activePlan = samplePlan,
            days = sampleDays,
            groceryList = sampleGroceries,
        )
        val initialState = MealPlanManagerState(content = successContent)

        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.ToggleGrocery(GroceryItemId("g-1")),
        )

        assertEquals(1, next.commands.size)
        assertEquals(
            MealPlanManagerCommand.ToggleGrocery(samplePlan.id, GroceryItemId("g-1")),
            next.commands.first(),
        )
    }

    @Test
    fun `Ui DeactivatePlan issues DeactivatePlan command`() {
        val initialState = MealPlanManagerState(userId = UserId("user-test"))
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.DeactivatePlan,
        )

        assertEquals(1, next.commands.size)
        assertEquals(MealPlanManagerCommand.DeactivatePlan(UserId("user-test")), next.commands.first())
    }

    @Test
    fun `Ui NavigateToCatalog produces NavigateToCatalog news`() {
        val initialState = MealPlanManagerState()
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Ui.NavigateToCatalog,
        )

        assertEquals(1, next.news.size)
        assertEquals(MealPlanManagerNews.NavigateToCatalog, next.news.first())
    }

    @Test
    fun `Internal Loaded with plan transitions to Success content`() {
        val initialState = MealPlanManagerState(content = MealPlanManagerState.Content.Loading)
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Internal.Loaded(
                activePlan = samplePlan,
                days = sampleDays,
                groceryList = sampleGroceries,
            ),
        )

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<MealPlanManagerState.Content.Success>(content)
        assertEquals(samplePlan, content.activePlan)
        assertEquals(1, content.days.size)
        assertEquals(1, content.groceryList.size)
    }

    @Test
    fun `Internal Loaded with null plan transitions to Empty content`() {
        val initialState = MealPlanManagerState(content = MealPlanManagerState.Content.Loading)
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Internal.Loaded(
                activePlan = null,
                days = emptyList(),
                groceryList = emptyList(),
            ),
        )

        val state = requireNotNull(next.state)
        assertIs<MealPlanManagerState.Content.Empty>(state.content)
    }

    @Test
    fun `Internal GroceryToggled updates bought status in grocery list`() {
        val successContent = MealPlanManagerState.Content.Success(
            activePlan = samplePlan,
            days = sampleDays,
            groceryList = sampleGroceries,
        )
        val initialState = MealPlanManagerState(content = successContent)

        val next = update.update(
            initialState,
            MealPlanManagerEvent.Internal.GroceryToggled(GroceryItemId("g-1"), true),
        )

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<MealPlanManagerState.Content.Success>(content)
        assertTrue(content.groceryList.first().isBought)
    }

    @Test
    fun `Internal PlanDeactivated transitions to Empty and shows snackbar`() {
        val initialState = MealPlanManagerState()
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Internal.PlanDeactivated,
        )

        val state = requireNotNull(next.state)
        assertIs<MealPlanManagerState.Content.Empty>(state.content)
        assertEquals(1, next.news.size)
        assertIs<MealPlanManagerNews.ShowSnackbar>(next.news.first())
    }

    @Test
    fun `Internal Failed transitions to Error content`() {
        val initialState = MealPlanManagerState(content = MealPlanManagerState.Content.Loading)
        val next = update.update(
            initialState,
            MealPlanManagerEvent.Internal.Failed(RuntimeException("Network failure")),
        )

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<MealPlanManagerState.Content.Error>(content)
        assertEquals("Network failure", content.message)
    }
}
