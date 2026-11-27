package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.UserId

sealed interface CoachStudentsCommand {
    data class LoadLinks(val coachUserId: UserId) : CoachStudentsCommand
    data class InviteStudent(
        val coachUserId: UserId,
        val studentId: UserId,
        val canViewDiary: Boolean,
    ) : CoachStudentsCommand
}
