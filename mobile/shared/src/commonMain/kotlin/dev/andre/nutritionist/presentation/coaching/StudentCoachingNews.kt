package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.domain.model.MealPlanTemplateId

sealed interface StudentCoachingNews {
    data class NavigateToMealPlan(val planId: MealPlanTemplateId? = null) : StudentCoachingNews
    data class ShowSnackbar(val message: String) : StudentCoachingNews
}
