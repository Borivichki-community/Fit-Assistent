package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealPlanId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogUpdateTest {

    private val update = CatalogUpdate()
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

    @Test
    fun `Load sets Loading state and emits LoadPlans command`() {
        val initial = CatalogState()
        val next = update.update(initial, CatalogEvent.Ui.Load)

        val state = requireNotNull(next.state)
        assertIs<CatalogState.Content.Loading>(state.content)
        assertEquals(listOf(CatalogCommand.LoadPlans("", "Все")), next.commands)
    }

    @Test
    fun `SearchQueryChanged updates query and emits LoadPlans command`() {
        val initial = CatalogState(selectedCategory = "Баланс")
        val next = update.update(initial, CatalogEvent.Ui.SearchQueryChanged("рыба"))

        val state = requireNotNull(next.state)
        assertEquals("рыба", state.query)
        assertIs<CatalogState.Content.Loading>(state.content)
        assertEquals(listOf(CatalogCommand.LoadPlans("рыба", "Баланс")), next.commands)
    }

    @Test
    fun `SearchQueryChanged when offline emits snackbar`() {
        val initial = CatalogState(isOnline = false)
        val next = update.update(initial, CatalogEvent.Ui.SearchQueryChanged("рыба"))

        assertEquals(
            listOf(CatalogNews.ShowSnackbar("Поиск в онлайн-каталоге недоступен без интернета")),
            next.news,
        )
    }

    @Test
    fun `NetworkStatusChanged updates isOnline in state`() {
        val initial = CatalogState(isOnline = true)
        val next = update.update(initial, CatalogEvent.Ui.NetworkStatusChanged(false))

        val state = requireNotNull(next.state)
        assertEquals(false, state.isOnline)
    }

    @Test
    fun `CategorySelected updates category and emits LoadPlans command`() {
        val initial = CatalogState(query = "сушка")
        val next = update.update(initial, CatalogEvent.Ui.CategorySelected("Похудение"))

        val state = requireNotNull(next.state)
        assertEquals("Похудение", state.selectedCategory)
        assertIs<CatalogState.Content.Loading>(state.content)
        assertEquals(listOf(CatalogCommand.LoadPlans("сушка", "Похудение")), next.commands)
    }

    @Test
    fun `PlanSelected sets selectedPlanId and emits NavigateToPlanDetail news`() {
        val initial = CatalogState()
        val next = update.update(initial, CatalogEvent.Ui.PlanSelected(samplePlan.id))

        val state = requireNotNull(next.state)
        assertEquals(samplePlan.id, state.selectedPlanId)
        assertEquals(listOf(CatalogNews.NavigateToPlanDetail(samplePlan.id)), next.news)
    }

    @Test
    fun `ToggleFavorite optimistically updates plan in state and emits ToggleFavorite command`() {
        val initial = CatalogState(content = CatalogState.Content.Success(listOf(samplePlan)))
        val next = update.update(initial, CatalogEvent.Ui.ToggleFavorite(samplePlan.id))

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<CatalogState.Content.Success>(content)
        assertTrue(content.plans.first().isFavorite)
        assertEquals(listOf(CatalogCommand.ToggleFavorite(samplePlan.id)), next.commands)
    }

    @Test
    fun `GenerateAiPlanClick emits NavigateToAiGenerator news`() {
        val initial = CatalogState()
        val next = update.update(initial, CatalogEvent.Ui.GenerateAiPlanClick)

        assertEquals(listOf(CatalogNews.NavigateToAiGenerator), next.news)
    }

    @Test
    fun `PlansLoaded sets Success content with plans`() {
        val initial = CatalogState(content = CatalogState.Content.Loading)
        val next = update.update(initial, CatalogEvent.Internal.PlansLoaded(listOf(samplePlan)))

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<CatalogState.Content.Success>(content)
        assertEquals(listOf(samplePlan), content.plans)
    }

    @Test
    fun `LoadFailed sets Error content`() {
        val initial = CatalogState(content = CatalogState.Content.Loading)
        val next = update.update(initial, CatalogEvent.Internal.LoadFailed(RuntimeException("Network error")))

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<CatalogState.Content.Error>(content)
        assertEquals("Network error", content.message)
    }
}
