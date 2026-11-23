package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.NutritionAnalytics
import dev.andre.nutritionist.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StatisticsUpdateTest {

    private val update = StatisticsUpdate()

    private val sampleAnalytics = NutritionAnalytics(
        period = AnalyticsPeriod.WEEK,
        averageDailyCalories = 2175,
        targetDailyCalories = 2200,
        calorieCompliancePercent = 96,
        averageProteinsGrams = 135.0,
        averageFatsGrams = 68.0,
        averageCarbsGrams = 245.0,
        totalSpentRub = 2580,
        totalBudgetRub = 2800,
        savedRub = 220,
        averageMealCostRub = 92,
        dailyPoints = emptyList(),
    )

    @Test
    fun init_sets_userId_loading_and_emits_command() {
        val initial = StatisticsState()
        val next = update.update(initial, StatisticsEvent.Ui.Init(UserId("user-123")))

        val state = requireNotNull(next.state)
        assertEquals(UserId("user-123"), state.userId)
        assertIs<StatisticsState.Content.Loading>(state.content)
        assertTrue(next.commands.contains(StatisticsCommand.LoadAnalytics(UserId("user-123"), AnalyticsPeriod.WEEK)))
    }

    @Test
    fun periodSelected_changes_period_and_emits_command() {
        val initial = StatisticsState(period = AnalyticsPeriod.WEEK, userId = UserId("user-123"))
        val next = update.update(initial, StatisticsEvent.Ui.PeriodSelected(AnalyticsPeriod.MONTH))

        val state = requireNotNull(next.state)
        assertEquals(AnalyticsPeriod.MONTH, state.period)
        assertIs<StatisticsState.Content.Loading>(state.content)
        assertTrue(next.commands.contains(StatisticsCommand.LoadAnalytics(UserId("user-123"), AnalyticsPeriod.MONTH)))
    }

    @Test
    fun periodSelected_same_period_does_nothing() {
        val initial = StatisticsState(period = AnalyticsPeriod.WEEK, userId = UserId("user-123"))
        val next = update.update(initial, StatisticsEvent.Ui.PeriodSelected(AnalyticsPeriod.WEEK))

        assertTrue(next.commands.isEmpty())
    }

    @Test
    fun retryClicked_sets_loading_and_emits_command() {
        val initial = StatisticsState(
            userId = UserId("user-123"),
            period = AnalyticsPeriod.WEEK,
            content = StatisticsState.Content.Error("Ошибка"),
        )
        val next = update.update(initial, StatisticsEvent.Ui.RetryClicked)

        val state = requireNotNull(next.state)
        assertIs<StatisticsState.Content.Loading>(state.content)
        assertTrue(next.commands.contains(StatisticsCommand.LoadAnalytics(UserId("user-123"), AnalyticsPeriod.WEEK)))
    }

    @Test
    fun analyticsLoaded_sets_Success_content() {
        val initial = StatisticsState(content = StatisticsState.Content.Loading)
        val next = update.update(initial, StatisticsEvent.Internal.AnalyticsLoaded(sampleAnalytics))

        val state = requireNotNull(next.state)
        val content = assertIs<StatisticsState.Content.Success>(state.content)
        assertEquals(sampleAnalytics, content.analytics)
    }

    @Test
    fun analyticsLoadFailed_sets_Error_content() {
        val initial = StatisticsState(content = StatisticsState.Content.Loading)
        val next = update.update(initial, StatisticsEvent.Internal.AnalyticsLoadFailed(RuntimeException("Network failure")))

        val state = requireNotNull(next.state)
        val content = assertIs<StatisticsState.Content.Error>(state.content)
        assertEquals("Network failure", content.message)
    }
}
