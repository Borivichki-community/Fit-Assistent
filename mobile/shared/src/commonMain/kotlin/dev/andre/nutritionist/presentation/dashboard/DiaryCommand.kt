package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.UserId

sealed interface DiaryCommand {
    data class ObserveDate(
        val dateEpochMs: Long,
        val userId: UserId? = null,
    ) : DiaryCommand

    data class DeleteRecord(val id: MealRecordId) : DiaryCommand
    data object SyncPending : DiaryCommand
}
