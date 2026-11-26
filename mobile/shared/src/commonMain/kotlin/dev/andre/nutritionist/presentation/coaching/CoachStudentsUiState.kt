package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.UserId

data class CoachStudentLinkUiModel(
    val linkId: String,
    val studentId: UserId,
    val studentLabel: String,
    val status: CoachLinkStatus,
    val statusLabel: String,
    val canViewDiary: Boolean,
    val diaryAccessLabel: String,
    val canOpenDiary: Boolean,
)

data class CoachStudentsUiState(
    val studentIdInput: String = "",
    val canViewDiary: Boolean = true,
    val isInviting: Boolean = false,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val links: List<CoachStudentLinkUiModel>) : Content
        data class Error(val message: String) : Content
    }
}
