package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.CoachLinkStatus
import dev.andre.nutritionist.domain.model.CoachStudentLink

class CoachStudentsUiStateMapper : UiStateMapper<CoachStudentsState, CoachStudentsUiState> {

    override fun map(state: CoachStudentsState): CoachStudentsUiState {
        val content = when (val c = state.content) {
            CoachStudentsState.Content.Loading -> CoachStudentsUiState.Content.Loading
            is CoachStudentsState.Content.Loaded -> CoachStudentsUiState.Content.Success(
                links = c.links.map { it.toUiModel() },
            )
            is CoachStudentsState.Content.Error -> CoachStudentsUiState.Content.Error(c.message)
        }
        return CoachStudentsUiState(
            studentIdInput = state.studentIdInput,
            canViewDiary = state.canViewDiary,
            isInviting = state.isInviting,
            content = content,
        )
    }

    private fun CoachStudentLink.toUiModel(): CoachStudentLinkUiModel = CoachStudentLinkUiModel(
        linkId = id.raw,
        studentId = studentId,
        studentLabel = studentId.raw,
        status = status,
        statusLabel = when (status) {
            CoachLinkStatus.PENDING -> "PENDING"
            CoachLinkStatus.ACTIVE -> "ACTIVE"
            CoachLinkStatus.REVOKED -> "REVOKED"
            CoachLinkStatus.UNSPECIFIED -> "UNSPECIFIED"
        },
        canViewDiary = canViewDiary,
        diaryAccessLabel = if (canViewDiary) "Доступ к дневнику: Да" else "Доступ к дневнику: Нет",
        canOpenDiary = status == CoachLinkStatus.ACTIVE && canViewDiary,
    )
}
