package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.datetime.LocalDate

/**
 * Асинхронные сайд-эффект команды главного экрана.
 */
sealed interface DashboardCommand {
    data class Load(val userId: UserId, val date: LocalDate) : DashboardCommand
    data class AddMeal(val entry: DiaryEntry) : DashboardCommand
    data class DeleteMeal(val entryId: DiaryEntryId, val userId: UserId, val date: LocalDate) : DashboardCommand
}
