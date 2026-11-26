package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.MealPlanTemplateId

data class StudentCoachInviteUiModel(
    val linkId: CoachLinkId,
    val coachLabel: String,
    val status: CoachLinkStatus,
    val canViewDiary: Boolean,
    val diaryAccessLabel: String,
    val isBusy: Boolean,
)

data class StudentPlanSummaryUiModel(
    val planId: MealPlanTemplateId,
    val title: String,
    val subtitle: String,
)

data class StudentCoachingUiState(
    val planIdInput: String = "",
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(
            val pendingInvites: List<StudentCoachInviteUiModel>,
            val activeCoaches: List<StudentCoachInviteUiModel>,
            val availablePlans: List<StudentPlanSummaryUiModel>,
        ) : Content

        data class Error(val message: String) : Content
    }
}
