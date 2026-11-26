package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.UserId

sealed interface CoachStudentsNews {
    data class NavigateToStudentMenu(val studentId: UserId) : CoachStudentsNews
    data object NavigateToMealPlans : CoachStudentsNews
    data class ShowSnackbar(val message: String) : CoachStudentsNews
}
