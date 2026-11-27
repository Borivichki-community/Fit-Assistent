package dev.andre.nutritionist.android.ui.screens.airecipe

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.airecipe.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiRecipeScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun configuring() {
        val state = AiRecipeUiStateMapper().map(AiRecipeState(AiRecipePrompt(MealType.LUNCH,500),0L))
        compose.setContent { AiRecipeScreen(state) {} }
        compose.onNodeWithText("Пожелания и ингредиенты").assertExists()
        compose.onNodeWithText("500").assertExists()
    }
    @Test fun generating() {
        compose.setContent { AiRecipeScreen(AiRecipeUiState("Обед",AiRecipeUiState.Content.Generating("Подбор",0.65f,"65%"))) {} }
        compose.onNodeWithContentDescription("Подбор").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "65%")).assertIsDisplayed()
    }
    @Test fun resultActions() {
        val events = mutableListOf<AiRecipeEvent.Ui>()
        val content = AiRecipeUiState.Content.Success("Каша","","400 ккал",emptyList(),emptyList(),"","Добавить в обед",true)
        compose.setContent { AiRecipeScreen(AiRecipeUiState("Обед",content)) { events.add(it) } }
        listOf("Добавить в обед","Перегенерировать","Отмена").forEach { compose.onNodeWithText(it).performClick() }
        assertEquals(listOf(AiRecipeEvent.Ui.AddRecipeToDiaryClicked,AiRecipeEvent.Ui.RegenerateClicked,AiRecipeEvent.Ui.CancelClicked),events)
    }
}
