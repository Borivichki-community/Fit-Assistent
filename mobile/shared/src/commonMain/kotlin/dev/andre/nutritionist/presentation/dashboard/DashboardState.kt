package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Неделимый снимок состояния главного экрана (Dashboard / Diary).
 * В соответствии с KoTEA и AGENTS.md §3.1 содержит вложенный sealed interface Content
 * без флагов isLoading/error на верхнем уровне.
 */
data class DashboardState(
    val content: Content = Content.Loading,
    val currentUserId: UserId? = null,
    val selectedDate: LocalDate? = null,
    val expandedMeals: Set<MealType> = setOf(MealType.DINNER),
    val showCompleteProfileBanner: Boolean = false,
) {
    sealed interface Content {
        data object Loading : Content

        data class Success(
            val profile: Profile?,
            val diary: DailyDiary,
            val expandedMeals: Set<MealType> = setOf(MealType.DINNER),
            val showCompleteProfileBanner: Boolean = false,
        ) : Content

        data class Error(val message: String) : Content
    }
}
