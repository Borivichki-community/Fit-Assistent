package dev.andre.nutritionist.presentation.statistics

/**
 * Одноразовые эффекты экрана аналитики.
 */
sealed interface StatisticsNews {
    data class ShowMessage(val message: String) : StatisticsNews
}
