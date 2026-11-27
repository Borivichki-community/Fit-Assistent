package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId

sealed interface StudentCoachingEvent {
    sealed interface Ui : StudentCoachingEvent {
        data class Load(val studentUserId: UserId) : Ui
        data object Retry : Ui
        data class DiaryConsentChanged(val linkId: CoachLinkId, val canViewDiary: Boolean) : Ui
        data class AcceptInviteClicked(val linkId: CoachLinkId) : Ui
        data class DeclineInviteClicked(val linkId: CoachLinkId) : Ui
        data class RevokeCoachClicked(val linkId: CoachLinkId) : Ui
        data class PlanIdInputChanged(val value: String) : Ui
        data class OpenPlanClicked(val planId: MealPlanTemplateId) : Ui
        data object OpenEnteredPlanClicked : Ui
        data object BrowsePlansClicked : Ui
    }

    sealed interface Internal : StudentCoachingEvent {
        data class DataLoaded(
            val links: List<CoachStudentLink>,
            val plans: List<MealPlanTemplate>,
        ) : Internal

        data class LoadFailed(val error: Throwable) : Internal
        data class InviteResponded(val updatedLink: CoachStudentLink) : Internal
        data class RespondFailed(val linkId: CoachLinkId, val error: Throwable) : Internal
        data class PlanVerified(val planId: MealPlanTemplateId) : Internal
        data class PlanOpenFailed(val error: Throwable) : Internal
    }
}
