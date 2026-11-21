package dev.andre.nutritionist.presentation.dashboard

/**
 * Разовые уведомления и сайд-эффекты для UI (навигация, snackbar).
 */
sealed interface DashboardNews {
    data class ShowSnackbar(val message: String) : DashboardNews
    data class NavigateToMealDetail(val mealId: String) : DashboardNews
}
