package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.DailyDiary
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Все события главного экрана (Dashboard / Diary).
 * Чётко разделены на намерения пользователя (Ui) и результаты асинхронных команд (Internal).
 */
sealed interface DashboardEvent {

    sealed interface Ui : DashboardEvent {
        data class LoadDashboard(
            val userId: UserId,
            val date: LocalDate,
            val showCompleteProfileBanner: Boolean = false,
        ) : Ui

        data class SelectDate(val date: LocalDate) : Ui

        data class ToggleMealSection(val mealType: MealType) : Ui

        data class AddMealOptimistic(val entry: DiaryEntry) : Ui

        data class DeleteMeal(val entryId: DiaryEntryId) : Ui

        data object Retry : Ui
    }

    sealed interface Internal : DashboardEvent {
        data class Loaded(
            val profile: Profile?,
            val diary: DailyDiary,
        ) : Internal

        data class LoadFailed(val error: Throwable) : Internal

        data class MealAdded(val entry: DiaryEntry) : Internal

        data class MealDeleted(val entryId: DiaryEntryId) : Internal

        data class MutationFailed(val error: Throwable) : Internal
    }
}
