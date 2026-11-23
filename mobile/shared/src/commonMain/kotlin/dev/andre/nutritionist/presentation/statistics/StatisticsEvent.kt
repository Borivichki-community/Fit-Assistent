package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.NutritionAnalytics
import dev.andre.nutritionist.domain.model.UserId

/**
 * События экрана аналитики питания и бюджета.
 */
sealed interface StatisticsEvent {

    sealed interface Ui : StatisticsEvent {
        data class Init(val userId: UserId) : Ui
        data class PeriodSelected(val period: AnalyticsPeriod) : Ui
        data object RetryClicked : Ui
    }

    sealed interface Internal : StatisticsEvent {
        data class AnalyticsLoaded(val analytics: NutritionAnalytics) : Internal
        data class AnalyticsLoadFailed(val throwable: Throwable) : Internal
    }
}
