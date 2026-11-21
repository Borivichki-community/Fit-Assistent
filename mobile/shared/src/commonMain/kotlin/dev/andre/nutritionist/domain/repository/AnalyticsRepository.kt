package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.AnalyticsPeriod
import dev.andre.nutritionist.domain.model.NutritionAnalytics
import dev.andre.nutritionist.domain.model.UserId

/**
 * Репозиторий аналитики питания и бюджета.
 */
interface AnalyticsRepository {
    suspend fun getNutritionAnalytics(userId: UserId, period: AnalyticsPeriod): Result<NutritionAnalytics>
}
