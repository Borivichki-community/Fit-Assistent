package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MenuUpdateTest {

    private val update = MenuUpdate()

    @Test
    fun `GenerateClicked sets Generating state and emits GenerateMenu command with current params`() {
        val initial = MenuState(
            period = TargetPeriod.DAY,
            targetCalories = 2200,
            budgetRub = 650.0,
            preferences = listOf("prod-1", "prod-2"),
        )

        val next = update.update(initial, MenuEvent.Ui.GenerateClicked)

        assertIs<MenuState.Content.Generating>(next.state?.content)
        val command = assertIs<MenuCommand.GenerateMenu>(next.commands.single())
        assertEquals(TargetPeriod.DAY, command.params.period)
        assertEquals(2200.0, command.params.targetCalories)
        assertEquals(65_000L, command.params.maxBudget.amountMinor)
        assertEquals(listOf(ProductId("prod-1"), ProductId("prod-2")), command.params.preferredProductIds)
    }

    @Test
    fun `changing parameters while Generating resets state to Idle and emits CancelGeneration`() {
        val generating = MenuState(content = MenuState.Content.Generating())

        val afterCalories = update.update(generating, MenuEvent.Ui.CaloriesChanged(1800))
        assertEquals(1800, afterCalories.state?.targetCalories)
        assertIs<MenuState.Content.Idle>(afterCalories.state?.content)
        assertEquals(listOf(MenuCommand.CancelGeneration), afterCalories.commands)

        val afterBudget = update.update(generating, MenuEvent.Ui.BudgetChanged(400.0))
        assertEquals(400.0, afterBudget.state?.budgetRub)
        assertEquals(listOf(MenuCommand.CancelGeneration), afterBudget.commands)

        val afterPeriod = update.update(generating, MenuEvent.Ui.PeriodSelected(TargetPeriod.LUNCH))
        assertEquals(TargetPeriod.LUNCH, afterPeriod.state?.period)
        assertEquals(listOf(MenuCommand.CancelGeneration), afterPeriod.commands)

        val afterPref = update.update(generating, MenuEvent.Ui.PreferenceToggled("p-1"))
        assertEquals(listOf("p-1"), afterPref.state?.preferences)
        assertEquals(listOf(MenuCommand.CancelGeneration), afterPref.commands)
    }

    @Test
    fun `CancelClicked sets Idle and emits CancelGeneration`() {
        val generating = MenuState(content = MenuState.Content.Generating())

        val next = update.update(generating, MenuEvent.Ui.CancelClicked)

        assertIs<MenuState.Content.Idle>(next.state?.content)
        assertEquals(listOf(MenuCommand.CancelGeneration), next.commands)
    }

    @Test
    fun `ProgressUpdated and GenerationCompleted update state to Generating label and Success`() {
        val generating = MenuState(content = MenuState.Content.Generating())

        val initStep = update.update(
            generating,
            MenuEvent.Internal.ProgressUpdated(MenuGenerationProgress.Initializing),
        )
        assertEquals(
            "Инициализация подбора меню...",
            assertIs<MenuState.Content.Generating>(initStep.state?.content).statusLabel,
        )

        val plan = MealPlan(
            id = "plan-1",
            period = TargetPeriod.DAY,
            meals = emptyList(),
            totalNutrition = NutritionFacts(calories = 1950.0, proteins = 130.0, fats = 60.0, carbs = 210.0),
            estimatedCost = Money.fromRubles(450.0),
        )
        val completed = update.update(generating, MenuEvent.Internal.GenerationCompleted(plan))
        val success = assertIs<MenuState.Content.Success>(completed.state?.content)
        assertEquals("plan-1", success.plan.id.raw)
        assertEquals("450 ₽", success.plan.estimatedCostLabel)
    }

    @Test
    fun `GenerationFailed sets Error state and emits ShowSnackbar news`() {
        val generating = MenuState(content = MenuState.Content.Generating())

        val next = update.update(
            generating,
            MenuEvent.Internal.GenerationFailed(RuntimeException("Превышен бюджет")),
        )

        val error = assertIs<MenuState.Content.Error>(next.state?.content)
        assertEquals("Превышен бюджет", error.message)
        assertEquals(listOf(MenuNews.ShowSnackbar("Превышен бюджет")), next.news)
    }
}
