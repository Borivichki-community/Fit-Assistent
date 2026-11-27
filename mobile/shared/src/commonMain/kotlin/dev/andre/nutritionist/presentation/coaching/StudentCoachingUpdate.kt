package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.CoachLinkId
import dev.andre.nutritionist.domain.model.MealPlanTemplateId

class StudentCoachingUpdate : DslUpdate<StudentCoachingState, StudentCoachingEvent, StudentCoachingCommand, StudentCoachingNews>() {

    override fun NextBuilder<StudentCoachingState, StudentCoachingCommand, StudentCoachingNews>.update(
        event: StudentCoachingEvent,
    ) {
        when (event) {
            is StudentCoachingEvent.Ui.Load -> {
                state { copy(currentUserId = event.studentUserId, content = StudentCoachingState.Content.Loading) }
                commands(StudentCoachingCommand.LoadData(event.studentUserId))
            }

            StudentCoachingEvent.Ui.Retry -> {
                state { copy(content = StudentCoachingState.Content.Loading) }
                commands(StudentCoachingCommand.LoadData(state.currentUserId))
            }

            is StudentCoachingEvent.Ui.DiaryConsentChanged -> {
                state {
                    copy(diaryConsentOverrides = diaryConsentOverrides + (event.linkId to event.canViewDiary))
                }
            }

            is StudentCoachingEvent.Ui.AcceptInviteClicked -> {
                respondInvite(event.linkId, accept = true)
            }

            is StudentCoachingEvent.Ui.DeclineInviteClicked -> {
                respondInvite(event.linkId, accept = false)
            }

            is StudentCoachingEvent.Ui.RevokeCoachClicked -> {
                respondInvite(event.linkId, accept = false)
            }

            is StudentCoachingEvent.Ui.PlanIdInputChanged -> {
                state { copy(planIdInput = event.value) }
            }

            is StudentCoachingEvent.Ui.OpenPlanClicked -> {
                commands(StudentCoachingCommand.OpenMealPlan(event.planId))
            }

            StudentCoachingEvent.Ui.OpenEnteredPlanClicked -> {
                val raw = state.planIdInput.trim()
                if (raw.isEmpty()) {
                    news(StudentCoachingNews.ShowSnackbar("Введите ID плана питания"))
                    return
                }
                commands(StudentCoachingCommand.OpenMealPlan(MealPlanTemplateId(raw)))
            }

            StudentCoachingEvent.Ui.BrowsePlansClicked -> {
                news(StudentCoachingNews.NavigateToMealPlan(null))
            }

            is StudentCoachingEvent.Internal.DataLoaded -> {
                state {
                    copy(
                        links = event.links,
                        availablePlans = event.plans,
                        content = StudentCoachingState.Content.Loaded,
                    )
                }
            }

            is StudentCoachingEvent.Internal.LoadFailed -> {
                state {
                    copy(
                        content = StudentCoachingState.Content.Error(
                            event.error.message ?: "Не удалось загрузить приглашения и доступы",
                        ),
                    )
                }
            }

            is StudentCoachingEvent.Internal.InviteResponded -> {
                val updated = state.links.map { existing ->
                    if (existing.id == event.updatedLink.id) event.updatedLink else existing
                }
                state {
                    copy(
                        busyLinkIds = busyLinkIds - event.updatedLink.id,
                        links = updated,
                        content = StudentCoachingState.Content.Loaded,
                    )
                }
                news(StudentCoachingNews.ShowSnackbar("Статус связи обновлён: ${event.updatedLink.status.name}"))
            }

            is StudentCoachingEvent.Internal.RespondFailed -> {
                state { copy(busyLinkIds = busyLinkIds - event.linkId) }
                news(StudentCoachingNews.ShowSnackbar(event.error.message ?: "Не удалось обновить приглашение"))
            }

            is StudentCoachingEvent.Internal.PlanVerified -> {
                news(StudentCoachingNews.NavigateToMealPlan(event.planId))
            }

            is StudentCoachingEvent.Internal.PlanOpenFailed -> {
                news(StudentCoachingNews.ShowSnackbar(event.error.message ?: "Не удалось открыть меню"))
            }
        }
    }

    private fun NextBuilder<StudentCoachingState, StudentCoachingCommand, StudentCoachingNews>.respondInvite(
        linkId: CoachLinkId,
        accept: Boolean,
    ) {
        if (linkId in state.busyLinkIds) return
        state { copy(busyLinkIds = busyLinkIds + linkId) }
        commands(StudentCoachingCommand.RespondInvite(linkId, accept = accept))
    }
}
