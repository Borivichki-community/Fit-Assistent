package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.MealRecordId

sealed interface DiaryCommand {
    data class ObserveDate(val dateEpochMs: Long) : DiaryCommand
    data class DeleteRecord(val id: MealRecordId) : DiaryCommand
    data object SyncPending : DiaryCommand
}
