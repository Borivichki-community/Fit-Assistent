package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.calculator.NutritionCalculator
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter

class CoachStudentMenuUiStateMapper : UiStateMapper<CoachStudentMenuState, CoachStudentMenuUiState> {

    override fun map(state: CoachStudentMenuState): CoachStudentMenuUiState {
        val dateLabel = if (state.dateEpochMs > 0L) {
            DateFormatter.formatDayHeader(state.dateEpochMs)
        } else {
            ""
        }
        val content = when (val c = state.content) {
            CoachStudentMenuState.Content.Loading -> CoachStudentMenuUiState.Content.Loading
            CoachStudentMenuState.Content.Loaded -> {
                val totalFacts = NutritionCalculator.sumNutrition(state.diaryRecords.map { it.nutrition })
                val factSummary = if (state.diaryRecords.isEmpty()) {
                    "Факт: записей за день нет"
                } else {
                    "Факт: ${NutritionFormatter.formatCalories(totalFacts.calories)} • " +
                        formatMacrosShort(totalFacts)
                }
                CoachStudentMenuUiState.Content.Success(
                    selectedPlanTitle = state.selectedPlan?.title,
                    selectedPlanSummary = state.selectedPlan?.let { plan ->
                        "${plan.durationDays} дн. • ${plan.accessMode.name} • ID: ${plan.id.raw}"
                    },
                    availablePlans = state.availablePlans.map { plan ->
                        CoachPlanOptionUiModel(
                            id = plan.id,
                            title = plan.title,
                            subtitle = "${plan.durationDays} дн. • ${plan.accessMode.name}",
                            isSelected = plan.id.raw == state.selectedPlanIdInput.trim(),
                        )
                    },
                    factSummaryLabel = factSummary,
                    diaryError = state.diaryError,
                    diaryEntries = state.diaryRecords.map { record ->
                        CoachDiaryEntryUiModel(
                            id = record.id.raw,
                            mealTypeLabel = mealTypeTitle(record.mealType),
                            title = record.nameSnapshot,
                            nutritionLabel = "${NutritionFormatter.formatGram(record.weightGrams)} • " +
                                "${NutritionFormatter.formatCalories(record.nutrition.calories)} • " +
                                formatMacrosShort(record.nutrition),
                        )
                    },
                    issuedGrants = state.issuedGrants.map { grant ->
                        CoachGrantedAccessUiModel(
                            grantId = grant.id,
                            summary = "${grant.targetType.name}: ${grant.targetId} (${grant.grantSource.name})",
                        )
                    },
                )
            }
            is CoachStudentMenuState.Content.Error -> CoachStudentMenuUiState.Content.Error(c.message)
        }
        return CoachStudentMenuUiState(
            studentIdLabel = state.studentId.raw,
            dateLabel = dateLabel,
            planIdInput = state.selectedPlanIdInput,
            selectedGrantSource = state.grantSource,
            expiresDaysInput = state.expiresDaysInput,
            isSubmittingGrant = state.isSubmittingGrant,
            content = content,
        )
    }

    private fun formatMacrosShort(facts: dev.andre.nutritionist.domain.model.NutritionFacts): String =
        "Б ${NutritionFormatter.formatMacroValue(facts.proteins)} • " +
            "Ж ${NutritionFormatter.formatMacroValue(facts.fats)} • " +
            "У ${NutritionFormatter.formatMacroValue(facts.carbs)}"

    private fun mealTypeTitle(mealType: MealType): String = when (mealType) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
        MealType.SNACK -> "Перекус"
    }
}
