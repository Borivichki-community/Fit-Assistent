package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.UserId

data class StudentCoachingState(
    val currentUserId: UserId = UserId(""),
    val planIdInput: String = "",
    val diaryConsentOverrides: Map<CoachLinkId, Boolean> = emptyMap(),
    val links: List<CoachStudentLink> = emptyList(),
    val availablePlans: List<MealPlanTemplate> = emptyList(),
    val busyLinkIds: Set<CoachLinkId> = emptySet(),
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data object Loaded : Content
        data class Error(val message: String) : Content
    }
}
