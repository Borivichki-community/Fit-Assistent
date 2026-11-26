package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.UserId

sealed interface StudentCoachingCommand {
    data class LoadData(val studentUserId: UserId) : StudentCoachingCommand
    data class RespondInvite(val linkId: CoachLinkId, val accept: Boolean) : StudentCoachingCommand
    data class OpenMealPlan(val planId: MealPlanTemplateId) : StudentCoachingCommand
}
