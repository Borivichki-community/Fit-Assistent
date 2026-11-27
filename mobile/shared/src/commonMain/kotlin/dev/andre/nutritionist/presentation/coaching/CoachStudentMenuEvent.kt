package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.UserId

sealed interface CoachStudentMenuEvent {
    sealed interface Ui : CoachStudentMenuEvent {
        data class Load(val studentId: UserId, val dateEpochMs: Long) : Ui
        data object Retry : Ui
        data object PreviousDayClicked : Ui
        data object NextDayClicked : Ui
        data class PlanIdChanged(val value: String) : Ui
        data class PlanSelected(val planId: MealPlanTemplateId) : Ui
        data class GrantSourceSelected(val source: GrantSource) : Ui
        data class ExpiresDaysChanged(val value: String) : Ui
        data object GrantAccessClicked : Ui
        data class RevokeAccessClicked(val grantId: AccessGrantId) : Ui
        data object OpenMealPlansClicked : Ui
    }

    sealed interface Internal : CoachStudentMenuEvent {
        data class Loaded(
            val availablePlans: List<MealPlanTemplate>,
            val selectedPlan: MealPlanTemplate?,
            val diaryRecords: List<MealRecord>,
            val diaryError: String?,
        ) : Internal

        data class DiaryLoaded(
            val records: List<MealRecord>,
            val error: String?,
        ) : Internal

        data class PlanLoaded(val plan: MealPlanTemplate) : Internal
        data class PlanLoadFailed(val error: Throwable) : Internal
        data class LoadFailed(val error: Throwable) : Internal
        data class AccessGranted(val grant: AccessGrant) : Internal
        data class GrantFailed(val error: Throwable) : Internal
        data class AccessRevoked(val grantId: AccessGrantId) : Internal
        data class RevokeFailed(val error: Throwable) : Internal
    }
}
