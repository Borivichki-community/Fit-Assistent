package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.DailyNutritionBalance
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate

sealed interface DiaryEvent {

    sealed interface Ui : DiaryEvent {
        data class DateSelected(
            val dateEpochMs: Long,
            val userId: UserId? = null,
        ) : Ui

        data object PreviousWeekClicked : Ui
        data object NextWeekClicked : Ui
        data object Retry : Ui
        data class ToggleMealSection(val mealType: MealType) : Ui
        data class DeleteRecord(val id: MealRecordId) : Ui
        data object ManualSyncClicked : Ui
    }

    sealed interface Internal : DiaryEvent {
        data class RecordsLoaded(
            val records: List<MealRecord>,
            val balance: DailyNutritionBalance,
            val totalCost: Money = Money.ZERO,
            val weeklyBudget: Double = 0.0,
            val targetCalories: Int = balance.targetCalories,
            val loggedDatesInWeek: Set<LocalDate> = emptySet(),
            val showCompleteProfileBanner: Boolean = false,
        ) : Internal

        data class SyncFinished(val syncedCount: Int) : Internal
        data class Failed(val error: Throwable) : Internal
    }
}
