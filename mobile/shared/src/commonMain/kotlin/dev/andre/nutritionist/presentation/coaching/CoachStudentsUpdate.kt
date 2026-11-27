package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.UserId

class CoachStudentsUpdate : DslUpdate<CoachStudentsState, CoachStudentsEvent, CoachStudentsCommand, CoachStudentsNews>() {

    override fun NextBuilder<CoachStudentsState, CoachStudentsCommand, CoachStudentsNews>.update(
        event: CoachStudentsEvent,
    ) {
        when (event) {
            is CoachStudentsEvent.Ui.Load -> {
                state { copy(currentUserId = event.coachUserId, content = CoachStudentsState.Content.Loading) }
                commands(CoachStudentsCommand.LoadLinks(event.coachUserId))
            }

            CoachStudentsEvent.Ui.Retry -> {
                if (state.currentUserId.raw.isNotBlank()) {
                    state { copy(content = CoachStudentsState.Content.Loading) }
                    commands(CoachStudentsCommand.LoadLinks(state.currentUserId))
                }
            }

            is CoachStudentsEvent.Ui.StudentIdChanged -> {
                state { copy(studentIdInput = event.value) }
            }

            is CoachStudentsEvent.Ui.CanViewDiaryChanged -> {
                state { copy(canViewDiary = event.enabled) }
            }

            CoachStudentsEvent.Ui.InviteClicked -> {
                val target = state.studentIdInput.trim()
                if (target.isEmpty()) {
                    news(CoachStudentsNews.ShowSnackbar("Введите ID или логин ученика"))
                    return
                }
                if (state.isInviting) return
                state { copy(isInviting = true) }
                commands(
                    CoachStudentsCommand.InviteStudent(
                        coachUserId = state.currentUserId,
                        studentId = UserId(target),
                        canViewDiary = state.canViewDiary,
                    ),
                )
            }

            is CoachStudentsEvent.Ui.OpenDiaryClicked -> {
                news(CoachStudentsNews.NavigateToStudentMenu(event.studentId))
            }

            is CoachStudentsEvent.Ui.AssignMenuClicked -> {
                news(CoachStudentsNews.NavigateToStudentMenu(event.studentId))
            }

            CoachStudentsEvent.Ui.OpenMealPlansClicked -> {
                news(CoachStudentsNews.NavigateToMealPlans)
            }

            is CoachStudentsEvent.Internal.LinksLoaded -> {
                state { copy(links = event.links, content = CoachStudentsState.Content.Loaded(event.links)) }
            }

            is CoachStudentsEvent.Internal.LinksFailed -> {
                state {
                    copy(
                        content = CoachStudentsState.Content.Error(
                            event.error.message ?: "Не удалось загрузить список учеников",
                        ),
                    )
                }
            }

            is CoachStudentsEvent.Internal.StudentInvited -> {
                val updated = listOf(event.link) + state.links.filterNot { it.id == event.link.id }
                state {
                    copy(
                        studentIdInput = "",
                        isInviting = false,
                        links = updated,
                        content = CoachStudentsState.Content.Loaded(updated),
                    )
                }
                news(CoachStudentsNews.ShowSnackbar("Приглашение отправлено: ${event.link.studentId.raw}"))
            }

            is CoachStudentsEvent.Internal.InviteFailed -> {
                state { copy(isInviting = false) }
                news(
                    CoachStudentsNews.ShowSnackbar(
                        event.error.message ?: "Не удалось отправить приглашение",
                    ),
                )
            }
        }
    }
}
