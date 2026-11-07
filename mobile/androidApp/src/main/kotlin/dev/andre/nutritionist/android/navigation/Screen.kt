package dev.andre.nutritionist.android.navigation

/**
 * Описание маршрутов навигации приложения (алиас для обратной совместимости).
 */
sealed interface Screen {
    data object Home : Screen
    data object Diary : Screen
    data object Recipes : Screen
    data object Profile : Screen
}
