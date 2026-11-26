package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.UserId

sealed interface CoachStudentsEvent {
    sealed interface Ui : CoachStudentsEvent {
        data class Load(val coachUserId: UserId) : Ui
        data object Retry : Ui
        data class StudentIdChanged(val value: String) : Ui
        data class CanViewDiaryChanged(val enabled: Boolean) : Ui
        data object InviteClicked : Ui
        data class OpenDiaryClicked(val studentId: UserId) : Ui
        data class AssignMenuClicked(val studentId: UserId) : Ui
        data object OpenMealPlansClicked : Ui
    }

    sealed interface Internal : CoachStudentsEvent {
        data class LinksLoaded(val links: List<CoachStudentLink>) : Internal
        data class LinksFailed(val error: Throwable) : Internal
        data class StudentInvited(val link: CoachStudentLink) : Internal
        data class InviteFailed(val error: Throwable) : Internal
    }
}
