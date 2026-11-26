package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink

class StudentCoachingUiStateMapper : UiStateMapper<StudentCoachingState, StudentCoachingUiState> {

    override fun map(state: StudentCoachingState): StudentCoachingUiState {
        val content = when (val c = state.content) {
            StudentCoachingState.Content.Loading -> StudentCoachingUiState.Content.Loading
            StudentCoachingState.Content.Loaded -> {
                val pending = state.links
                    .filter { it.status == CoachLinkStatus.PENDING }
                    .map { it.toUiModel(state) }
                val active = state.links
                    .filter { it.status == CoachLinkStatus.ACTIVE }
                    .map { it.toUiModel(state) }
                val plans = state.availablePlans.map { plan ->
                    StudentPlanSummaryUiModel(
                        planId = plan.id,
                        title = plan.title,
                        subtitle = "${plan.durationDays} дн. • ${plan.accessMode.name} • ID: ${plan.id.raw}",
                    )
                }
                StudentCoachingUiState.Content.Success(
                    pendingInvites = pending,
                    activeCoaches = active,
                    availablePlans = plans,
                )
            }
            is StudentCoachingState.Content.Error -> StudentCoachingUiState.Content.Error(c.message)
        }
        return StudentCoachingUiState(
            planIdInput = state.planIdInput,
            content = content,
        )
    }

    private fun CoachStudentLink.toUiModel(state: StudentCoachingState): StudentCoachInviteUiModel {
        val consent = state.diaryConsentOverrides[id] ?: canViewDiary
        return StudentCoachInviteUiModel(
            linkId = id,
            coachLabel = "Тренер: ${coachId.raw}",
            status = status,
            canViewDiary = consent,
            diaryAccessLabel = if (consent) "Доступ к дневнику разрешён" else "Только без дневника",
            isBusy = id in state.busyLinkIds,
        )
    }
}
