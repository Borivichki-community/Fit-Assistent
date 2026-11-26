package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId

sealed interface CoachStudentMenuCommand {
    data class LoadAll(val studentId: UserId, val dateEpochMs: Long, val planId: MealPlanTemplateId?) : CoachStudentMenuCommand
    data class LoadDiary(val studentId: UserId, val dateEpochMs: Long) : CoachStudentMenuCommand
    data class LoadPlan(val planId: MealPlanTemplateId) : CoachStudentMenuCommand
    data class GrantAccess(
        val studentId: UserId,
        val planId: String,
        val grantSource: GrantSource,
        val expiresInDays: Int?,
    ) : CoachStudentMenuCommand

    data class RevokeAccess(val grantId: AccessGrantId) : CoachStudentMenuCommand
}
