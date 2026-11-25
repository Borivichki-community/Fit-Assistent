package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.core.kotea.UiStateMapper
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter

class MenuUiStateMapper : UiStateMapper<MenuState, MenuUiState> {

    override fun map(state: MenuState): MenuUiState {
        val uiContent = when (val c = state.content) {
            MenuState.Content.Idle -> MenuUiState.Content.Idle
            is MenuState.Content.Generating -> MenuUiState.Content.Generating(c.statusLabel)
            is MenuState.Content.Success -> MenuUiState.Content.Success(c.plan)
            is MenuState.Content.Error -> MenuUiState.Content.Error(c.message)
        }
        val sanitizedBudget = if (state.budgetRub.isNaN()) 0.0 else state.budgetRub

        return MenuUiState(
            selectedPeriod = state.period,
            periodLabel = state.period.toLabel(),
            targetCalories = state.targetCalories,
            targetCaloriesLabel = NutritionFormatter.formatCalories(state.targetCalories.toDouble()),
            budgetRub = state.budgetRub,
            budgetLabel = MoneyFormatter.format(Money.fromRubles(sanitizedBudget)),
            preferences = state.preferences,
            content = uiContent,
        )
    }

    private fun TargetPeriod.toLabel(): String = when (this) {
        TargetPeriod.UNSPECIFIED -> "Не указано"
        TargetPeriod.DAY -> "День"
        TargetPeriod.BREAKFAST -> "Завтрак"
        TargetPeriod.LUNCH -> "Обед"
        TargetPeriod.DINNER -> "Ужин"
        TargetPeriod.SNACK -> "Перекус"
    }
}
