package dev.andre.nutritionist.android.ui.screens.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.andre.nutritionist.domain.model.MealPlanId
import dev.andre.nutritionist.presentation.catalog.CatalogSuccessUiModel
import dev.andre.nutritionist.presentation.catalog.CatalogUiState
import dev.andre.nutritionist.presentation.catalog.MealPlanCardUiModel
import dev.andre.nutritionist.presentation.catalog.PlanCategoryUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CatalogScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val samplePlan = MealPlanCardUiModel(
        id = "plan-1",
        title = "Средиземноморский баланс",
        description = "Рыба, оливковое масло, свежие овощи и цельные злаки.",
        category = "Баланс",
        caloriesBadge = "2100 ккал/день",
        budgetBadge = "450 ₽/день",
        macroSummaryText = "Б: 110г | Ж: 75г | У: 240г",
        ratingAndDifficultyText = "★ 4.9 · Средняя сложность",
        isFavorite = false,
        tags = listOf("Рыба", "Овощи"),
    )

    private val sampleSuccessData = CatalogSuccessUiModel(
        searchQuery = "",
        categories = listOf(
            PlanCategoryUiModel(title = "Все", isSelected = true),
            PlanCategoryUiModel(title = "Баланс", isSelected = false),
            PlanCategoryUiModel(title = "Эконом", isSelected = false),
        ),
        plans = listOf(samplePlan),
    )

    @Test
    fun `loading state displays progress indicator`() {
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Loading),
            )
        }

        composeTestRule.onNodeWithTag("catalog_loading").assertIsDisplayed()
    }

    @Test
    fun `error state displays error message and retry button invokes callback`() {
        var retried = false
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Error("Ошибка сети")),
                onRetryClick = { retried = true },
            )
        }

        composeTestRule.onNodeWithTag("catalog_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ошибка сети").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_retry_button").performClick()
        assertTrue(retried)
    }

    @Test
    fun `success state displays AI hero card and click invokes callback`() {
        var generateAiClicked = false
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Success(sampleSuccessData)),
                onGenerateAiPlanClick = { generateAiClicked = true },
            )
        }

        composeTestRule.onNodeWithTag("catalog_ai_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Персональный AI-рацион").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_generate_ai_button").performClick()
        assertTrue(generateAiClicked)
    }

    @Test
    fun `success state displays search input and category chips and clicking chip invokes callback`() {
        var selectedCategory = ""
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Success(sampleSuccessData)),
                onCategorySelected = { selectedCategory = it },
            )
        }

        composeTestRule.onNodeWithTag("catalog_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_category_chip_Баланс").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_category_chip_Баланс").performClick()
        assertEquals("Баланс", selectedCategory)
    }



    @Test
    fun `empty plans state displays not found message`() {
        val emptyData = sampleSuccessData.copy(plans = emptyList())
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Success(emptyData)),
            )
        }

        composeTestRule.onNodeWithTag("catalog_empty_plans").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Рационы не найдены").assertIsDisplayed()
    }

    @Test
    fun `plan card renders details and select button invokes callback`() {
        var selectedPlanId: MealPlanId? = null
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Success(sampleSuccessData)),
                onPlanSelected = { selectedPlanId = it },
            )
        }

        composeTestRule.onNodeWithTag("catalog_plan_card_plan-1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Средиземноморский баланс").assertIsDisplayed()
        composeTestRule.onNodeWithText("2100 ккал/день").assertIsDisplayed()
        composeTestRule.onNodeWithText("450 ₽/день").assertIsDisplayed()

        composeTestRule.onNodeWithTag("catalog_select_plan_button_plan-1").performScrollTo().performClick()
        assertEquals(MealPlanId("plan-1"), selectedPlanId)
    }

    @Test
    fun `clicking favorite button on plan card invokes callback`() {
        var toggledPlanId: MealPlanId? = null
        composeTestRule.setContent {
            CatalogScreen(
                state = CatalogUiState(content = CatalogUiState.Content.Success(sampleSuccessData)),
                onToggleFavorite = { toggledPlanId = it },
            )
        }

        composeTestRule.onNodeWithTag("catalog_favorite_button_plan-1").performScrollTo().performClick()
        assertEquals(MealPlanId("plan-1"), toggledPlanId)
    }
}
