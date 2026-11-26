package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.AccessMode
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter

class MealPlansUiStateMapper : UiStateMapper<MealPlansState, MealPlansUiState> {

    override fun map(state: MealPlansState): MealPlansUiState {
        val content = when (val c = state.content) {
            MealPlansState.Content.Loading -> MealPlansUiState.Content.Loading
            MealPlansState.Content.Loaded -> {
                val selectedDetail = state.selectedPlan?.let { plan ->
                    val daysSummary = if (plan.days.isEmpty()) {
                        "Элементы расписания не заполнены"
                    } else {
                        plan.days.joinToString("\n") { day ->
                            val names = day.items.joinToString { it.nameSnapshot }.ifBlank { "без блюд" }
                            "День ${day.dayNumber}: $names (${NutritionFormatter.formatCalories(day.totalNutrition.calories)})"
                        }
                    }
                    "${plan.durationDays} дн. • ${accessLabel(plan)} • ${plan.visibility.name}\n$daysSummary"
                }
                MealPlansUiState.Content.Success(
                    selectedPlanTitle = state.selectedPlan?.title,
                    selectedPlanDetail = selectedDetail,
                    plans = state.plans.map { it.toCardUiModel() },
                    recipes = state.recipes.map { it.toCardUiModel() },
                )
            }
            is MealPlansState.Content.Error -> MealPlansUiState.Content.Error(c.message)
        }

        return MealPlansUiState(
            isCoach = state.isCoach,
            titleInput = state.titleInput,
            descriptionInput = state.descriptionInput,
            durationDays = state.durationDays,
            selectedAccessMode = state.accessMode,
            priceRubInput = state.priceRubInput,
            searchQuery = state.searchQuery,
            isSubmitting = state.isSubmitting,
            content = content,
        )
    }

    private fun MealPlanTemplate.toCardUiModel(): MealPlanCardUiModel {
        val hasDay1Items = days.any { it.dayNumber == 1 && it.items.isNotEmpty() }
        return MealPlanCardUiModel(
            id = id,
            title = title,
            metaLabel = "$durationDays дн. • ${accessLabel(this)} • ${visibility.name}",
            idLabel = "planId: ${id.raw}",
            isPublic = visibility == ContentVisibility.PUBLIC,
            hasItemsForDayOne = hasDay1Items,
        )
    }

    private fun Recipe.toCardUiModel(): PublicRecipeCardUiModel = PublicRecipeCardUiModel(
        id = id,
        title = title,
        metaLabel = "${NutritionFormatter.formatCalories(nutrition.calories)} / 100 г • ${visibility.name}",
        isPublic = visibility == ContentVisibility.PUBLIC,
    )

    private fun accessLabel(plan: MealPlanTemplate): String = when (plan.accessMode) {
        AccessMode.FREE -> "FREE"
        AccessMode.COACH_ASSIGNED -> "COACH_ASSIGNED"
        AccessMode.PAID -> "PAID (${MoneyFormatter.format(plan.price)})"
        AccessMode.UNSPECIFIED -> "UNSPECIFIED"
    }
}
