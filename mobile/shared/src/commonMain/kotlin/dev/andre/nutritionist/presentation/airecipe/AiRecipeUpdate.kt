package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.AiRecipePrompt

/**
 * Чистая функция обновления состояния экрана генерации AI-рецепта в архитектуре KoTEA MVI.
 * Запрещены побочные эффекты и ветка else.
 */
class AiRecipeUpdate : DslUpdate<AiRecipeState, AiRecipeEvent, AiRecipeCommand, AiRecipeNews>() {

    override fun NextBuilder<AiRecipeState, AiRecipeCommand, AiRecipeNews>.update(event: AiRecipeEvent) {
        when (event) {
            is AiRecipeEvent.Ui -> handleUiEvent(event)
            is AiRecipeEvent.Internal -> handleInternalEvent(event)
        }
    }

    private fun NextBuilder<AiRecipeState, AiRecipeCommand, AiRecipeNews>.handleUiEvent(event: AiRecipeEvent.Ui) {
        when (event) {
            is AiRecipeEvent.Ui.QueryChanged -> {
                val current = state.content
                if (current is AiRecipeState.Content.Configuring) {
                    state { copy(content = current.copy(prompt = current.prompt.copy(query = event.query))) }
                }
            }
            is AiRecipeEvent.Ui.CaloriesChanged -> {
                val current = state.content
                if (current is AiRecipeState.Content.Configuring) {
                    state { copy(content = current.copy(prompt = current.prompt.copy(targetCalories = event.calories))) }
                }
            }
            is AiRecipeEvent.Ui.BudgetChanged -> {
                val current = state.content
                if (current is AiRecipeState.Content.Configuring) {
                    state { copy(content = current.copy(prompt = current.prompt.copy(targetBudgetRub = event.budgetRub))) }
                }
            }
            is AiRecipeEvent.Ui.MealTypeSelected -> {
                val current = state.content
                if (current is AiRecipeState.Content.Configuring) {
                    state { copy(content = current.copy(prompt = current.prompt.copy(mealType = event.mealType))) }
                }
            }
            is AiRecipeEvent.Ui.DietaryPreferenceSelected -> {
                val current = state.content
                if (current is AiRecipeState.Content.Configuring) {
                    state { copy(content = current.copy(prompt = current.prompt.copy(dietaryPreference = event.preference))) }
                }
            }
            AiRecipeEvent.Ui.GenerateClicked -> {
                val current = state.content
                if (current is AiRecipeState.Content.Configuring) {
                    state {
                        copy(
                            content = AiRecipeState.Content.Generating(
                                prompt = current.prompt,
                                phaseText = "Анализируем нутриенты...",
                                progress = 0.2f,
                            ),
                        )
                    }
                    commands(AiRecipeCommand.Generate(current.prompt))
                }
            }
            AiRecipeEvent.Ui.SaveRecipeClicked -> {
                val current = state.content
                if (current is AiRecipeState.Content.Success) {
                    commands(AiRecipeCommand.SaveRecipe(current.recipe))
                }
            }
            AiRecipeEvent.Ui.AddRecipeToDiaryClicked -> {
                val current = state.content
                if (current is AiRecipeState.Content.Success) {
                    commands(AiRecipeCommand.AddRecipeToDiary(current.recipe))
                }
            }
            AiRecipeEvent.Ui.ResetClicked -> {
                state { copy(content = AiRecipeState.Content.Configuring(AiRecipePrompt())) }
            }
            AiRecipeEvent.Ui.RetryClicked -> {
                val current = state.content
                if (current is AiRecipeState.Content.Error) {
                    val prompt = current.lastPrompt ?: AiRecipePrompt()
                    state {
                        copy(
                            content = AiRecipeState.Content.Generating(
                                prompt = prompt,
                                phaseText = "Повторяем попытку генерации...",
                                progress = 0.2f,
                            ),
                        )
                    }
                    commands(AiRecipeCommand.Generate(prompt))
                }
            }
        }
    }

    private fun NextBuilder<AiRecipeState, AiRecipeCommand, AiRecipeNews>.handleInternalEvent(event: AiRecipeEvent.Internal) {
        when (event) {
            is AiRecipeEvent.Internal.GenerationProgressUpdated -> {
                val current = state.content
                if (current is AiRecipeState.Content.Generating) {
                    state {
                        copy(
                            content = current.copy(
                                phaseText = event.phaseText,
                                progress = event.progress,
                            ),
                        )
                    }
                }
            }
            is AiRecipeEvent.Internal.RecipeGenerated -> {
                state { copy(content = AiRecipeState.Content.Success(recipe = event.recipe, isSaved = false)) }
            }
            is AiRecipeEvent.Internal.RecipeSaved -> {
                val current = state.content
                if (current is AiRecipeState.Content.Success) {
                    state { copy(content = current.copy(isSaved = true)) }
                    news(AiRecipeNews.ShowSnackbar("Рецепт сохранён в избранное"))
                }
            }
            AiRecipeEvent.Internal.RecipeAddedToDiary -> {
                news(AiRecipeNews.ShowSnackbar("Блюдо добавлено в дневник питания"))
                news(AiRecipeNews.RecipeAddedToDiarySuccess)
            }
            is AiRecipeEvent.Internal.GenerationFailed -> {
                val current = state.content
                val lastPrompt = when (current) {
                    is AiRecipeState.Content.Generating -> current.prompt
                    is AiRecipeState.Content.Configuring -> current.prompt
                    is AiRecipeState.Content.Error -> current.lastPrompt
                    is AiRecipeState.Content.Success -> null
                }
                state {
                    copy(
                        content = AiRecipeState.Content.Error(
                            message = event.error.message ?: "Не удалось сгенерировать рецепт",
                            lastPrompt = lastPrompt,
                        ),
                    )
                }
            }
        }
    }
}
