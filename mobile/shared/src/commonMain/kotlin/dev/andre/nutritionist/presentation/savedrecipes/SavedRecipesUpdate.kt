package dev.andre.nutritionist.presentation.savedrecipes

import dev.andre.nutritionist.core.kotea.DslUpdate

/**
 * Чистая MVI-редукция (State Machine) экрана сохранённых рецептов.
 * Не содержит побочных эффектов и веток else.
 */
class SavedRecipesUpdate : DslUpdate<SavedRecipesState, SavedRecipesEvent, SavedRecipesCommand, SavedRecipesNews>() {

    override fun NextBuilder<SavedRecipesState, SavedRecipesCommand, SavedRecipesNews>.update(event: SavedRecipesEvent) {
        when (event) {
            is SavedRecipesEvent.Ui -> handleUiEvent(event)
            is SavedRecipesEvent.Internal -> handleInternalEvent(event)
        }
    }

    private fun NextBuilder<SavedRecipesState, SavedRecipesCommand, SavedRecipesNews>.handleUiEvent(event: SavedRecipesEvent.Ui) {
        when (event) {
            is SavedRecipesEvent.Ui.Init -> {
                commands(SavedRecipesCommand.ObserveSavedRecipes)
            }

            is SavedRecipesEvent.Ui.SearchQueryChanged -> {
                val newFilter = state.filter.copy(query = event.query)
                state { copy(filter = newFilter) }
            }

            is SavedRecipesEvent.Ui.MealTypeFilterSelected -> {
                val newFilter = state.filter.copy(selectedMealType = event.mealType)
                state { copy(filter = newFilter) }
            }

            is SavedRecipesEvent.Ui.RecipeClicked -> {
                val current = state.content
                if (current is SavedRecipesState.Content.Success) {
                    val clicked = current.allRecipes.find { it.id == event.recipeId }
                    if (clicked != null) {
                        state { copy(content = current.copy(selectedRecipe = clicked)) }
                    }
                }
            }

            is SavedRecipesEvent.Ui.RecipeDetailDismissed -> {
                val current = state.content
                if (current is SavedRecipesState.Content.Success) {
                    state { copy(content = current.copy(selectedRecipe = null)) }
                }
            }

            is SavedRecipesEvent.Ui.ToggleFavoriteClicked -> {
                commands(SavedRecipesCommand.ToggleFavorite(event.recipeId))
            }

            is SavedRecipesEvent.Ui.AddRecipeToDiaryClicked -> {
                val current = state.content
                if (current is SavedRecipesState.Content.Success) {
                    val recipe = current.allRecipes.find { it.id == event.recipeId } ?: current.selectedRecipe
                    if (recipe != null) {
                        commands(SavedRecipesCommand.AddRecipeToDiary(recipe))
                    }
                }
            }

            is SavedRecipesEvent.Ui.RetryClicked -> {
                state { copy(content = SavedRecipesState.Content.Loading) }
                commands(SavedRecipesCommand.ObserveSavedRecipes)
            }

            is SavedRecipesEvent.Ui.GenerateRecipeClicked -> {
                news(SavedRecipesNews.NavigateToAiGenerator)
            }
        }
    }

    private fun NextBuilder<SavedRecipesState, SavedRecipesCommand, SavedRecipesNews>.handleInternalEvent(event: SavedRecipesEvent.Internal) {
        when (event) {
            is SavedRecipesEvent.Internal.RecipesLoaded -> {
                if (event.recipes.isEmpty()) {
                    state { copy(content = SavedRecipesState.Content.Empty(state.filter)) }
                } else {
                    val currentSelected = when (val c = state.content) {
                        is SavedRecipesState.Content.Success -> c.selectedRecipe?.let { sel ->
                            event.recipes.find { it.id == sel.id }
                        }
                        is SavedRecipesState.Content.Loading -> null
                        is SavedRecipesState.Content.Empty -> null
                        is SavedRecipesState.Content.Error -> null
                    }
                    state {
                        copy(
                            content = SavedRecipesState.Content.Success(
                                allRecipes = event.recipes,
                                selectedRecipe = currentSelected,
                            ),
                        )
                    }
                }
            }

            is SavedRecipesEvent.Internal.RecipesLoadFailed -> {
                state {
                    copy(
                        content = SavedRecipesState.Content.Error(
                            event.throwable.message ?: "Не удалось загрузить сохранённые рецепты",
                        ),
                    )
                }
            }

            is SavedRecipesEvent.Internal.RecipeToggled -> {
                val status = if (event.recipe.isFavorite) "добавлен в избранное" else "удален из избранного"
                val snackbarMessage = if (event.recipe.isFavorite) "Рецепт добавлен в избранное" else "Рецепт удалён из избранного"
                news(
                    SavedRecipesNews.ShowSnackbar(snackbarMessage),
                    SavedRecipesNews.ShowMessage("Рецепт «${event.recipe.title}» $status"),
                )
            }

            is SavedRecipesEvent.Internal.ToggleFailed -> {
                news(SavedRecipesNews.ShowSnackbar(event.throwable.message ?: "Ошибка изменения статуса"))
            }

            is SavedRecipesEvent.Internal.RecipeAddedToDiary -> {
                news(
                    SavedRecipesNews.ShowSnackbar("Блюдо добавлено в дневник"),
                    SavedRecipesNews.ShowMessage("Рецепт добавлен в дневник питания"),
                )
            }

            is SavedRecipesEvent.Internal.AddToDiaryFailed -> {
                news(SavedRecipesNews.ShowMessage(event.throwable.message ?: "Не удалось добавить блюдо в дневник"))
            }
        }
    }
}
