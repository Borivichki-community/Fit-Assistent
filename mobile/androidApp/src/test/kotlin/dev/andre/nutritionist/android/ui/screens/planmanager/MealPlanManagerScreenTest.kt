package dev.andre.nutritionist.android.ui.screens.planmanager

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.domain.model.GroceryItemId
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.planmanager.GroceryCategoryUiModel
import dev.andre.nutritionist.presentation.planmanager.GroceryItemUiModel
import dev.andre.nutritionist.presentation.planmanager.MealPlanManagerUiState
import dev.andre.nutritionist.presentation.planmanager.PlanDayUiModel
import dev.andre.nutritionist.presentation.planmanager.PlanManagerTab
import dev.andre.nutritionist.presentation.planmanager.PlanMealUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MealPlanManagerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMeals = listOf(
        PlanMealUiModel(
            title = "Овсяная каша с ягодами",
            mealType = "Завтрак",
            caloriesText = "420 ккал",
            proteinsText = "Б: 14г",
            fatsText = "Ж: 12г",
            carbsText = "У: 65г",
        ),
    )

    private val sampleDays = listOf(
        PlanDayUiModel(
            dayNumber = 1,
            dayName = "Понедельник",
            shortDayName = "Пн",
            targetCaloriesText = "2 190 ккал",
            isSelected = true,
            meals = sampleMeals,
        ),
        PlanDayUiModel(
            dayNumber = 2,
            dayName = "Вторник",
            shortDayName = "Вт",
            targetCaloriesText = "2 210 ккал",
            isSelected = false,
            meals = sampleMeals,
        ),
    )

    private val sampleGroceryCategories = listOf(
        GroceryCategoryUiModel(
            categoryName = "Овощи и зелень",
            countText = "1 поз.",
            items = listOf(
                GroceryItemUiModel(
                    id = GroceryItemId("g-1"),
                    name = "Авокадо Хасс",
                    amount = "3 шт",
                    estimatedPriceText = "350 ₽",
                    isBought = false,
                ),
            ),
        ),
    )

    private val sampleSuccessContent = MealPlanManagerUiState.Content.Success(
        planId = MealPlanId("plan-1"),
        title = "Средиземноморский баланс",
        category = "Баланс",
        difficulty = "Легко",
        durationText = "7 дней",
        targetCaloriesText = "2 200 ккал/день",
        weeklyBudgetText = "4 500 ₽/нед",
        proteinsText = "130 г",
        fatsText = "75 г",
        carbsText = "250 г",
        selectedTab = PlanManagerTab.SCHEDULE,
        days = sampleDays,
        selectedDayIndex = 0,
        currentDay = sampleDays.first(),
        groceryCategories = sampleGroceryCategories,
        totalGroceryItemsCount = 1,
        boughtGroceryItemsCount = 0,
        groceryProgressPercent = 0f,
        groceryProgressText = "Куплено 0 из 1",
        totalGroceryPriceText = "350 ₽",
    )

    @Test
    fun loading_state_displays_loading_indicator() {
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(content = MealPlanManagerUiState.Content.Loading),
                onSelectTab = {},
                onSelectDay = {},
                onToggleGrocery = {},
                onDeactivatePlan = {},
                onNavigateToCatalog = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("plan_manager_loading").assertIsDisplayed()
    }

    @Test
    fun empty_state_displays_message_and_navigates_to_catalog() {
        var catalogClicked = false
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(
                    content = MealPlanManagerUiState.Content.Empty(
                        message = "У вас пока нет активного плана питания",
                    ),
                ),
                onSelectTab = {},
                onSelectDay = {},
                onToggleGrocery = {},
                onDeactivatePlan = {},
                onNavigateToCatalog = { catalogClicked = true },
                onRetry = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("У вас пока нет активного плана питания").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_navigate_to_catalog_button").performClick()
        assertTrue(catalogClicked)
    }

    @Test
    fun error_state_displays_error_message_and_retry_button() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var retryClicked = false
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(
                    content = MealPlanManagerUiState.Content.Error("Ошибка сети"),
                ),
                onSelectTab = {},
                onSelectDay = {},
                onToggleGrocery = {},
                onDeactivatePlan = {},
                onNavigateToCatalog = {},
                onRetry = { retryClicked = true },
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithText("Ошибка сети").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.retry_button)).performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun success_state_displays_active_plan_hero_card() {
        var deactivateClicked = false
        var changeClicked = false
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(content = sampleSuccessContent),
                onSelectTab = {},
                onSelectDay = {},
                onToggleGrocery = {},
                onDeactivatePlan = { deactivateClicked = true },
                onNavigateToCatalog = { changeClicked = true },
                onRetry = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("active_plan_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Средиземноморский баланс").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 200 ккал/день").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 500 ₽/нед").assertIsDisplayed()

        composeTestRule.onNodeWithTag("deactivate_plan_button").performClick()
        assertTrue(deactivateClicked)

        composeTestRule.onNodeWithTag("change_plan_button").performClick()
        assertTrue(changeClicked)
    }

    @Test
    fun tabs_switching_calls_onSelectTab() {
        var selectedTab: PlanManagerTab? = null
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(content = sampleSuccessContent),
                onSelectTab = { selectedTab = it },
                onSelectDay = {},
                onToggleGrocery = {},
                onDeactivatePlan = {},
                onNavigateToCatalog = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("tab_groceries").performScrollTo().performClick()
        assertEquals(PlanManagerTab.GROCERY_LIST, selectedTab)
    }

    @Test
    fun day_selection_calls_onSelectDay() {
        var selectedDay: Int? = null
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(content = sampleSuccessContent),
                onSelectTab = {},
                onSelectDay = { selectedDay = it },
                onToggleGrocery = {},
                onDeactivatePlan = {},
                onNavigateToCatalog = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("day_selector_pill_1").performScrollTo().performClick()
        assertEquals(1, selectedDay)
    }

    @Test
    fun grocery_item_toggle_calls_onToggleGrocery() {
        var toggledId: GroceryItemId? = null
        val groceryState = sampleSuccessContent.copy(selectedTab = PlanManagerTab.GROCERY_LIST)

        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(content = groceryState),
                onSelectTab = {},
                onSelectDay = {},
                onToggleGrocery = { toggledId = it },
                onDeactivatePlan = {},
                onNavigateToCatalog = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }

        composeTestRule.onNodeWithTag("grocery_item_g-1").performScrollTo().performClick()
        assertEquals(GroceryItemId("g-1"), toggledId)
    }

    @Test
    fun back_button_calls_onNavigateBack() {
        var backClicked = false
        composeTestRule.setContent {
            MealPlanManagerScreen(
                state = MealPlanManagerUiState(content = sampleSuccessContent),
                onSelectTab = {},
                onSelectDay = {},
                onToggleGrocery = {},
                onDeactivatePlan = {},
                onNavigateToCatalog = {},
                onRetry = {},
                onNavigateBack = { backClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("plan_manager_back_button").performClick()
        assertTrue(backClicked)
    }
}
