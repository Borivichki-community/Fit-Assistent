package dev.andre.nutritionist.presentation.statistics

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.NutritionAnalytics
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.AnalyticsRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class StatisticsCommandsFlowHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = object : CoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var analyticsRepository: FakeAnalyticsRepository
    private lateinit var handler: StatisticsCommandsFlowHandler

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

    @BeforeTest
    fun setUp() {
        analyticsRepository = FakeAnalyticsRepository()
        handler = StatisticsCommandsFlowHandler(
            analyticsRepository = analyticsRepository,
            dispatchers = testDispatchers,
        )
    }

    @Test
    fun loadAnalytics_emits_AnalyticsLoaded_on_success() = runTest(testDispatcher) {
        analyticsRepository.analyticsResult = Result.success(sampleAnalytics)

        handler.handle(flowOf(StatisticsCommand.LoadAnalytics(UserId("user-1"), AnalyticsPeriod.WEEK))).test {
            val event = awaitItem()
            val loaded = assertIs<StatisticsEvent.Internal.AnalyticsLoaded>(event)
            assertEquals(sampleAnalytics, loaded.analytics)
            awaitComplete()
        }
    }

    @Test
    fun loadAnalytics_emits_AnalyticsLoadFailed_on_error() = runTest(testDispatcher) {
        val error = RuntimeException("Analytics database failure")
        analyticsRepository.analyticsResult = Result.failure(error)

        handler.handle(flowOf(StatisticsCommand.LoadAnalytics(UserId("user-1"), AnalyticsPeriod.WEEK))).test {
            val event = awaitItem()
            val failed = assertIs<StatisticsEvent.Internal.AnalyticsLoadFailed>(event)
            assertEquals(error, failed.throwable)
            awaitComplete()
        }
    }

    private class FakeAnalyticsRepository : AnalyticsRepository {
        var analyticsResult: Result<NutritionAnalytics> = Result.failure(NotImplementedError())

        override suspend fun getNutritionAnalytics(
            userId: UserId,
            period: AnalyticsPeriod,
        ): Result<NutritionAnalytics> = analyticsResult
    }
}
