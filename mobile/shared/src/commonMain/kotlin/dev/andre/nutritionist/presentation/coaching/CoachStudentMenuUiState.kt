package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.AccessGrantId
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.MealPlanTemplateId

data class CoachPlanOptionUiModel(
    val id: MealPlanTemplateId,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
)

data class CoachDiaryEntryUiModel(
    val id: String,
    val mealTypeLabel: String,
    val title: String,
    val nutritionLabel: String,
)

data class CoachGrantedAccessUiModel(
    val grantId: AccessGrantId,
    val summary: String,
)

data class CoachStudentMenuUiState(
    val studentIdLabel: String = "",
    val dateLabel: String = "",
    val planIdInput: String = "",
    val selectedGrantSource: GrantSource = GrantSource.COACH_GRANT,
    val expiresDaysInput: String = "30",
    val isSubmittingGrant: Boolean = false,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(
            val selectedPlanTitle: String?,
            val selectedPlanSummary: String?,
            val availablePlans: List<CoachPlanOptionUiModel>,
            val factSummaryLabel: String,
            val diaryError: String?,
            val diaryEntries: List<CoachDiaryEntryUiModel>,
            val issuedGrants: List<CoachGrantedAccessUiModel>,
        ) : Content

        data class Error(val message: String) : Content
    }
}
