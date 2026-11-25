package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import dev.andre.nutritionist.presentation.mappers.MealPlanUiMapper.toUiModel

class MenuUpdate : DslUpdate<MenuState, MenuEvent, MenuCommand, MenuNews>() {

    override fun NextBuilder<MenuState, MenuCommand, MenuNews>.update(event: MenuEvent) {
        when (event) {
            is MenuEvent.Ui.CaloriesChanged -> {
                val wasGenerating = state.content is MenuState.Content.Generating
                state {
                    copy(
                        targetCalories = event.calories,
                        content = if (wasGenerating) MenuState.Content.Idle else content,
                    )
                }
                if (wasGenerating) {
                    commands(MenuCommand.CancelGeneration)
                }
            }

            is MenuEvent.Ui.BudgetChanged -> {
                val wasGenerating = state.content is MenuState.Content.Generating
                state {
                    copy(
                        budgetRub = event.budgetRub,
                        content = if (wasGenerating) MenuState.Content.Idle else content,
                    )
                }
                if (wasGenerating) {
                    commands(MenuCommand.CancelGeneration)
                }
            }

            is MenuEvent.Ui.PeriodSelected -> {
                val wasGenerating = state.content is MenuState.Content.Generating
                state {
                    copy(
                        period = event.period,
                        content = if (wasGenerating) MenuState.Content.Idle else content,
                    )
                }
                if (wasGenerating) {
                    commands(MenuCommand.CancelGeneration)
                }
            }

            is MenuEvent.Ui.PreferenceToggled -> {
                val wasGenerating = state.content is MenuState.Content.Generating
                val updatedPrefs = if (event.preferenceId in state.preferences) {
                    state.preferences - event.preferenceId
                } else {
                    state.preferences + event.preferenceId
                }
                state {
                    copy(
                        preferences = updatedPrefs,
                        content = if (wasGenerating) MenuState.Content.Idle else content,
                    )
                }
                if (wasGenerating) {
                    commands(MenuCommand.CancelGeneration)
                }
            }

            MenuEvent.Ui.GenerateClicked -> {
                val sanitizedBudget = if (state.budgetRub.isNaN()) 0.0 else state.budgetRub
                val params = GenerateBudgetMenuParams(
                    period = state.period,
                    targetCalories = state.targetCalories.toDouble(),
                    maxBudget = Money.fromRubles(sanitizedBudget),
                    preferredProductIds = state.preferences.map(::ProductId),
                )
                state { copy(content = MenuState.Content.Generating()) }
                commands(MenuCommand.GenerateMenu(params))
            }

            MenuEvent.Ui.CancelClicked -> {
                state { copy(content = MenuState.Content.Idle) }
                commands(MenuCommand.CancelGeneration)
            }

            is MenuEvent.Internal.ProgressUpdated -> {
                when (val progress = event.progress) {
                    MenuGenerationProgress.Initializing -> {
                        state { copy(content = MenuState.Content.Generating("Инициализация подбора меню...")) }
                    }
                    is MenuGenerationProgress.StreamingDay -> {
                        state {
                            copy(
                                content = MenuState.Content.Generating("Генерируем день ${progress.dayNumber}..."),
                            )
                        }
                    }
                    is MenuGenerationProgress.Completed -> {
                        state { copy(content = MenuState.Content.Success(progress.plan.toUiModel())) }
                    }
                }
            }

            is MenuEvent.Internal.GenerationCompleted -> {
                state { copy(content = MenuState.Content.Success(event.plan.toUiModel())) }
            }

            is MenuEvent.Internal.GenerationFailed -> {
                val message = event.error.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось сгенерировать меню"
                state { copy(content = MenuState.Content.Error(message)) }
                news(MenuNews.ShowSnackbar(message))
            }
        }
    }
}
