package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.*
import kotlin.test.*
import dev.andre.nutritionist.fake.*
import dev.andre.nutritionist.domain.usecase.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class AiRecipeCommandsFlowHandlerTest {
    @Test
    fun generatesAndSavesSelectedPortion() = runTest {
        val dispatchers = TestCoroutineDispatchers()
        val recipe = Recipe(RecipeId("recipe"), "Каша", totalWeightGrams = 200.0, nutrition = NutritionFacts(200.0, 10.0, 4.0, 31.0), estimatedCost = Money.fromMinor(15000))
        val recipes = FakeRecipeRepository().apply {
            planToReturn = MealPlan("plan",TargetPeriod.DINNER,listOf(recipe),recipe.nutrition,recipe.estimatedCost)
        }
        val catalog = FakeCatalogRepository()
        val handler = AiRecipeCommandsFlowHandler(GenerateBudgetMenuUseCase(recipes,catalog,dispatchers),catalog,
            LogRecipeToDiaryUseCase(FakeDiaryRepository(),dispatchers))
        val events = handler.handle(flowOf(AiRecipeCommand.Generate(AiRecipePrompt(MealType.DINNER,400)))).toList()
        assertIs<AiRecipeEvent.Internal.ProgressUpdated>(events.first())
        assertEquals(recipe,assertIs<AiRecipeEvent.Internal.RecipeGenerated>(events.last()).recipe)
        val saved = handler.handle(flowOf(AiRecipeCommand.AddRecipeToDiary(recipe,MealType.DINNER,1000L))).toList().single()
        val record = assertIs<AiRecipeEvent.Internal.RecipeAddedToDiary>(saved).record
        assertEquals(1000L,record.recordedAt.toEpochMilliseconds())
        assertEquals(400.0,record.nutrition.calories)
        assertEquals(SyncStatus.PENDING,record.syncStatus)
    }
}
