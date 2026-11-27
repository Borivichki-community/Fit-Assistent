package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachStudentLink
import dev.andre.nutritionist.domain.model.UserId

data class CoachStudentsState(
    val currentUserId: UserId = UserId(""),
    val studentIdInput: String = "",
    val canViewDiary: Boolean = true,
    val isInviting: Boolean = false,
    val links: List<CoachStudentLink> = emptyList(),
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Loaded(val links: List<CoachStudentLink>) : Content
        data class Error(val message: String) : Content
    }
}
