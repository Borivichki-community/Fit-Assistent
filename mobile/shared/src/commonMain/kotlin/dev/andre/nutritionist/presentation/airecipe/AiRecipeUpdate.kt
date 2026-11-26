package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.presentation.airecipe.AiRecipeState.Content
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent.Ui
import dev.andre.nutritionist.presentation.airecipe.AiRecipeEvent.Internal

class AiRecipeUpdate : DslUpdate<AiRecipeState, AiRecipeEvent, AiRecipeCommand, AiRecipeNews>() {
    override fun NextBuilder<AiRecipeState, AiRecipeCommand, AiRecipeNews>.update(event: AiRecipeEvent) {
        val current = state.content
        when (event) {
            is Ui.QueryChanged -> if (current == Content.Configuring) state { copy(prompt = prompt.copy(query = event.query)) }
            is Ui.CaloriesChanged -> if (current == Content.Configuring) state { copy(prompt = prompt.copy(targetCalories = event.calories)) }
            is Ui.FitRemainingChanged -> if (current == Content.Configuring) {
                state { copy(prompt = prompt.copy(fitRemaining = event.enabled)) }
            }
            is Ui.BudgetChanged -> if (current == Content.Configuring) state { copy(prompt = prompt.copy(targetBudgetRub = event.budgetRub)) }
            is Ui.MealTypeSelected -> if (current == Content.Configuring) state { copy(prompt = prompt.copy(mealType = event.mealType)) }
            is Ui.DietaryPreferenceSelected -> if (current == Content.Configuring) state { copy(prompt = prompt.copy(dietaryPreference = event.preference.takeUnless { it == prompt.dietaryPreference }.orEmpty())) }
            Ui.GenerateClicked, Ui.RegenerateClicked -> {
                if (current is Content.Generating || current is Content.Success && current.isSaving) return
                commands(AiRecipeCommand.Generate(state.prompt))
                state { copy(content = Content.Generating("Подготавливаем запрос…", 0f)) }
            }
            Ui.AddRecipeToDiaryClicked -> if (current is Content.Success && !current.isSaving) {
                commands(AiRecipeCommand.AddRecipeToDiary(current.recipe, state.prompt.mealType, state.dateEpochMs))
                state { copy(content = current.copy(isSaving = true)) }
            }
            Ui.CancelClicked, Ui.BackClicked -> {
                if (current is Content.Generating) {
                    commands(AiRecipeCommand.Cancel)
                    state { copy(content = Content.Configuring) }
                } else if (current !is Content.Success || !current.isSaving) {
                    news(AiRecipeNews.Dismiss)
                }
            }
            is Internal.ProgressUpdated -> if (current is Content.Generating) {
                state { copy(content = Content.Generating(event.phaseText, event.progress)) }
            }
            is Internal.RecipeGenerated -> if (current is Content.Generating) state { copy(content = Content.Success(event.recipe)) }
            is Internal.GenerationFailed -> if (current is Content.Generating) {
                state { copy(content = Content.Error(when (event.error) {
                    is dev.andre.nutritionist.domain.error.DomainError.Unauthorized -> "Не удалось подтвердить вход. Попробуйте войти снова."
                    else -> "Не удалось сгенерировать блюдо. Попробуйте ещё раз."
                })) }
            }
            is Internal.RecipeAddedToDiary -> news(AiRecipeNews.RecipeAddedSuccess)
            is Internal.SaveFailed -> if (current is Content.Success) {
                state { copy(content = current.copy(isSaving = false)) }
                news(AiRecipeNews.ShowSnackbar(event.error.message ?: "Не удалось сохранить блюдо"))
            }
        }
    }
}
