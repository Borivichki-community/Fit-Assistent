package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.NutritionAnalytics
import dev.andre.nutritionist.domain.model.UserId

/**
 * MVI-состояние экрана аналитики питания и бюджета.
 */
data class StatisticsState(
    val userId: UserId = UserId("current_user"),
    val period: AnalyticsPeriod = AnalyticsPeriod.WEEK,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val analytics: NutritionAnalytics) : Content
        data class Error(val message: String) : Content
    }
}
