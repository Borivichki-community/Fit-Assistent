package dev.andre.nutritionist.presentation.coaching

sealed interface CoachStudentMenuNews {
    data object NavigateToMealPlans : CoachStudentMenuNews
    data class ShowSnackbar(val message: String) : CoachStudentMenuNews
}
