package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.DailyNutritionBalance
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId

sealed interface DiaryEvent {

    sealed interface Ui : DiaryEvent {
        data class DateSelected(val dateEpochMs: Long) : Ui
        data class DeleteRecord(val id: MealRecordId) : Ui
        data object ManualSyncClicked : Ui
    }

    sealed interface Internal : DiaryEvent {
        data class RecordsLoaded(
            val records: List<MealRecord>,
            val balance: DailyNutritionBalance,
        ) : Internal

        data class SyncFinished(val syncedCount: Int) : Internal
        data class Failed(val error: Throwable) : Internal
    }
}
