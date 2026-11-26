package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.presentation.addmeal.AddMealItemMapper.toSearchItemUiModel

/**
 * Чистый синхронный редьюсер меню выбора продукта или рецепта (@fitapp_4).
 */
class AddMealUpdate : DslUpdate<AddMealState, AddMealEvent, AddMealCommand, AddMealNews>() {

    override fun NextBuilder<AddMealState, AddMealCommand, AddMealNews>.update(event: AddMealEvent) {
        when (event) {
            is AddMealEvent.Ui.Open -> {
                state {
                    AddMealState(
                        mealType = event.mealType,
                        selectedDateEpochMs = event.dateEpochMs,
                        dailyTargetCalories = event.dailyTargetCalories,
                        content = AddMealState.Content.Idle,
                    )
                }
            }

            is AddMealEvent.Ui.QueryChanged -> {
                val trimmed = event.query.trim()
                if (trimmed.isEmpty()) {
                    state {
                        copy(
                            query = event.query,
                            items = emptyList(),
                            domainProducts = emptyMap(),
                            domainRecipes = emptyMap(),
                            content = AddMealState.Content.Idle,
                        )
                    }
                } else {
                    state {
                        copy(
                            query = event.query,
                            content = AddMealState.Content.Loading,
                        )
                    }
                    commands(AddMealCommand.Search(query = trimmed))
                }
            }

            AddMealEvent.Ui.ClearQuery -> {
                state {
                    copy(
                        query = "",
                        items = emptyList(),
                        domainProducts = emptyMap(),
                        domainRecipes = emptyMap(),
                        content = AddMealState.Content.Idle,
                    )
                }
            }

            AddMealEvent.Ui.Retry -> {
                val trimmed = state.query.trim()
                if (trimmed.isNotEmpty()) {
                    state { copy(content = AddMealState.Content.Loading) }
                    commands(AddMealCommand.Search(query = trimmed, forceRefresh = true))
                }
            }

            is AddMealEvent.Ui.ItemClicked -> {
                if (state.content is AddMealState.Content.Success &&
                    state.content.isSaving
                ) {
                    return
                }
                when (event.kind) {
                    AddMealItemKind.PRODUCT -> {
                        val product = state.domainProducts[ProductId(event.id)]
                        if (product != null) {
                            state { copy(content = AddMealState.Content.Success(isSaving = true)) }
                            commands(
                                AddMealCommand.AddProduct(
                                    product = product,
                                    mealType = state.mealType,
                                    dateEpochMs = state.selectedDateEpochMs,
                                ),
                            )
                        }
                    }
                    AddMealItemKind.RECIPE -> {
                        val recipe = state.domainRecipes[RecipeId(event.id)]
                        if (recipe != null) {
                            state { copy(content = AddMealState.Content.Success(isSaving = true)) }
                            commands(
                                AddMealCommand.AddRecipe(
                                    recipe = recipe,
                                    mealType = state.mealType,
                                    dateEpochMs = state.selectedDateEpochMs,
                                ),
                            )
                        }
                    }
                }
            }

            AddMealEvent.Ui.Dismiss -> {
                news(AddMealNews.Dismiss)
            }

            AddMealEvent.Ui.AiGenerateClicked -> {
                val share = when (state.mealType) {
                    dev.andre.nutritionist.domain.model.MealType.BREAKFAST -> 0.25
                    dev.andre.nutritionist.domain.model.MealType.LUNCH -> 0.35
                    dev.andre.nutritionist.domain.model.MealType.DINNER -> 0.30
                    dev.andre.nutritionist.domain.model.MealType.SNACK -> 0.10
                }
                news(AddMealNews.NavigateToAiGenerator(state.mealType, state.selectedDateEpochMs, (state.dailyTargetCalories * share).toInt().coerceAtLeast(1)))
            }

            AddMealEvent.Ui.TakePhotoClicked,
            AddMealEvent.Ui.VoiceAssistantClicked,
            AddMealEvent.Ui.SavedFoodClicked,
            -> {
                // Кнопки-заглушки по спецификации @fitapp_4
            }

            is AddMealEvent.Internal.SearchLoaded -> {
                if (state.query.trim() != event.query) return
                val productMap = event.products.associateBy { it.id }
                val recipeMap = event.recipes.associateBy { it.id }
                val uiItems = event.products.map { it.toSearchItemUiModel() } +
                    event.recipes.map { it.toSearchItemUiModel() }
                state {
                    copy(
                        items = uiItems,
                        domainProducts = productMap,
                        domainRecipes = recipeMap,
                        content = if (uiItems.isEmpty()) {
                            AddMealState.Content.Empty
                        } else {
                            AddMealState.Content.Success(isSaving = false)
                        },
                    )
                }
            }

            is AddMealEvent.Internal.SearchFailed -> {
                if (state.query.isBlank()) return
                val message = event.error.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось выполнить поиск продуктов и рецептов"
                state {
                    copy(
                        items = emptyList(),
                        content = AddMealState.Content.Error(message),
                    )
                }
            }

            is AddMealEvent.Internal.ItemSaved -> {
                news(
                    AddMealNews.MealAdded(event.record),
                    AddMealNews.Dismiss,
                )
            }

            is AddMealEvent.Internal.SaveFailed -> {
                val message = event.error.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось добавить блюдо в дневник"
                state {
                    if (items.isNotEmpty()) {
                        copy(content = AddMealState.Content.Success(isSaving = false))
                    } else {
                        copy(content = AddMealState.Content.Error(message))
                    }
                }
                news(AddMealNews.ShowSnackbar(message))
            }
        }
    }
}
