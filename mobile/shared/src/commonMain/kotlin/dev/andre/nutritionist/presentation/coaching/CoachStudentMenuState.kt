package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.AccessGrant
import dev.andre.nutritionist.domain.model.GrantSource
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.UserId

data class CoachStudentMenuState(
    val studentId: UserId = UserId(""),
    val dateEpochMs: Long = 0L,
    val selectedPlanIdInput: String = "",
    val grantSource: GrantSource = GrantSource.COACH_GRANT,
    val expiresDaysInput: String = "30",
    val selectedPlan: MealPlanTemplate? = null,
    val availablePlans: List<MealPlanTemplate> = emptyList(),
    val diaryRecords: List<MealRecord> = emptyList(),
    val diaryError: String? = null,
    val issuedGrants: List<AccessGrant> = emptyList(),
    val isSubmittingGrant: Boolean = false,
    val content: Content = Content.Loading,
) {
    sealed interface Content {
        data object Loading : Content
        data object Loaded : Content
        data class Error(val message: String) : Content
    }
}
