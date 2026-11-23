package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.UserId

/**
 * Команды экрана аналитики для выполнения в фоновом потоке.
 */
sealed interface StatisticsCommand {
    data class LoadAnalytics(
        val userId: UserId,
        val period: AnalyticsPeriod,
    ) : StatisticsCommand
}
