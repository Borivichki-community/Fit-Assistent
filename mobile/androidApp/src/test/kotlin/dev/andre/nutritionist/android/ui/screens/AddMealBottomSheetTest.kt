package dev.andre.nutritionist.android.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.presentation.addmeal.AddMealSuccessUiModel
import dev.andre.nutritionist.presentation.addmeal.FoodCategoryUiModel
import dev.andre.nutritionist.presentation.addmeal.FoodItemUiModel
import dev.andre.nutritionist.presentation.addmeal.PortionSummaryUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AddMealBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val sampleFoods = listOf(
        FoodItemUiModel(
            id = "food-1",
            name = "Овсяная каша",
            brand = "Традиционная",
            caloriesPer100g = 68,
            caloriesDescription = "68 ккал / 100 г",
            macroDescription = "Б: 2.4 г • Ж: 1.4 г • У: 12 г",
            isSelected = false,
        ),
        FoodItemUiModel(
            id = "food-2",
            name = "Куриное филе",
            brand = "Петелинка",
            caloriesPer100g = 165,
            caloriesDescription = "165 ккал / 100 г",
            macroDescription = "Б: 31 г • Ж: 3.6 г • У: 0 г",
            isSelected = true,
        ),
    )

    private val sampleCategories = listOf(
        FoodCategoryUiModel(title = "Все", isSelected = true),
        FoodCategoryUiModel(title = "Завтрак", isSelected = false),
        FoodCategoryUiModel(title = "Обед", isSelected = false),
    )

    private val samplePortion = PortionSummaryUiModel(
        portionGrams = 150.0,
        portionGramsLabel = "150 г",
        caloriesTotal = 248,
        proteinsGrams = 46.5,
        fatsGrams = 5.4,
        carbsGrams = 0.0,
        caloriesLabel = "248 ккал",
        macroSummaryText = "Б: 46.5 г • Ж: 5.4 г • У: 0 г",
    )

    @Test
    fun `sheet displays title with meal type and search input`() {
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = null,
            portionSummary = null,
            addButtonText = "Выберите продукт",
            isAddEnabled = false,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = {},
                onCategorySelect = {},
                onFoodSelect = {},
                onPortionChange = {},
                onConfirmAdd = {},
                onDismiss = {},
            )
        }

        val expectedTitle = context.getString(R.string.add_meal_title_format, "Обед")
        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_meal_search_input").assertIsDisplayed()
    }

    @Test
    fun `entering search text triggers onQueryChange`() {
        var query = ""
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = null,
            portionSummary = null,
            addButtonText = "Выберите продукт",
            isAddEnabled = false,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = { query = it },
                onCategorySelect = {},
                onFoodSelect = {},
                onPortionChange = {},
                onConfirmAdd = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("add_meal_search_input").performTextInput("каша")
        assertEquals("каша", query)
    }

    @Test
    fun `clicking category pill triggers onCategorySelect`() {
        var selectedCat: String? = null
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = null,
            portionSummary = null,
            addButtonText = "Выберите продукт",
            isAddEnabled = false,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = {},
                onCategorySelect = { selectedCat = it },
                onFoodSelect = {},
                onPortionChange = {},
                onConfirmAdd = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("categories_row").performScrollTo()
        composeTestRule.onNodeWithTag("category_tab_Завтрак").performClick()
        assertEquals("Завтрак", selectedCat)
    }

    @Test
    fun `clicking food item invokes onFoodSelect callback`() {
        var clickedFood: FoodItemUiModel? = null
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = null,
            portionSummary = null,
            addButtonText = "Выберите продукт",
            isAddEnabled = false,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = {},
                onCategorySelect = {},
                onFoodSelect = { clickedFood = it },
                onPortionChange = {},
                onConfirmAdd = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("food_item_food-1").performScrollTo().performClick()
        assertEquals("food-1", clickedFood?.id)
    }

    @Test
    fun `portion presets are displayed and clicking preset triggers onPortionChange`() {
        var chosenGrams: Double? = null
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = sampleFoods[1],
            portionSummary = samplePortion,
            addButtonText = "Добавить в Обед • 248 ккал",
            isAddEnabled = true,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = {},
                onCategorySelect = {},
                onFoodSelect = {},
                onPortionChange = { chosenGrams = it },
                onConfirmAdd = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("portion_preset_200").performScrollTo().performClick()
        assertEquals(200.0, chosenGrams)
    }

    @Test
    fun `confirm add button is enabled and triggers callback when isAddEnabled is true`() {
        var confirmed = false
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = sampleFoods[1],
            portionSummary = samplePortion,
            addButtonText = "Добавить в Обед • 248 ккал",
            isAddEnabled = true,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = {},
                onCategorySelect = {},
                onFoodSelect = {},
                onPortionChange = {},
                onConfirmAdd = { confirmed = true },
                onDismiss = {},
            )
        }

        val btn = composeTestRule.onNodeWithTag("add_meal_confirm_btn")
        btn.assertIsEnabled()
        btn.performScrollTo().performClick()
        assertTrue(confirmed)
    }

    @Test
    fun `confirm add button is disabled when isAddEnabled is false`() {
        val data = AddMealSuccessUiModel(
            mealTypeTitle = "Обед",
            searchQuery = "",
            categories = sampleCategories,
            foods = sampleFoods,
            selectedFood = null,
            portionSummary = null,
            addButtonText = "Выберите продукт",
            isAddEnabled = false,
        )

        composeTestRule.setContent {
            AddMealSuccessContent(
                data = data,
                onQueryChange = {},
                onCategorySelect = {},
                onFoodSelect = {},
                onPortionChange = {},
                onConfirmAdd = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("add_meal_confirm_btn").assertIsNotEnabled()
    }
}
