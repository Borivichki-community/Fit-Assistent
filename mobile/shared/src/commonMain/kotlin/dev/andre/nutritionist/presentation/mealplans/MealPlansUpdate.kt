package dev.andre.nutritionist.presentation.mealplans

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.AccessMode

class MealPlansUpdate : DslUpdate<MealPlansState, MealPlansEvent, MealPlansCommand, MealPlansNews>() {

    override fun NextBuilder<MealPlansState, MealPlansCommand, MealPlansNews>.update(event: MealPlansEvent) {
        when (event) {
            is MealPlansEvent.Ui.Load -> {
                state {
                    copy(
                        currentUserId = event.userId,
                        isCoach = event.isCoach,
                        initialPlanId = event.initialPlanId,
                        content = MealPlansState.Content.Loading,
                    )
                }
                commands(MealPlansCommand.LoadAll(state.searchQuery.trim(), event.initialPlanId))
            }

            MealPlansEvent.Ui.Retry,
            MealPlansEvent.Ui.SearchClicked,
            -> {
                state { copy(content = MealPlansState.Content.Loading) }
                commands(MealPlansCommand.LoadAll(state.searchQuery.trim(), state.initialPlanId))
            }

            is MealPlansEvent.Ui.TitleChanged -> state { copy(titleInput = event.value) }
            is MealPlansEvent.Ui.DescriptionChanged -> state { copy(descriptionInput = event.value) }
            is MealPlansEvent.Ui.DurationDaysSelected -> state { copy(durationDays = event.days.coerceIn(1, 7)) }
            is MealPlansEvent.Ui.AccessModeSelected -> {
                val allowed = if (!state.isCoach && event.mode != AccessMode.FREE) AccessMode.FREE else event.mode
                state { copy(accessMode = allowed) }
            }

            is MealPlansEvent.Ui.PriceRubChanged -> {
                state { copy(priceRubInput = event.value.filter(Char::isDigit).take(6)) }
            }

            is MealPlansEvent.Ui.SearchQueryChanged -> state { copy(searchQuery = event.value) }

            MealPlansEvent.Ui.CreatePlanClicked -> {
                val title = state.titleInput.trim()
                if (title.isEmpty()) {
                    news(MealPlansNews.ShowSnackbar("Введите название меню"))
                    return
                }
                if (state.isSubmitting) return
                val priceMinor = if (state.accessMode == AccessMode.PAID) {
                    (state.priceRubInput.toLongOrNull() ?: 0L) * 100L
                } else {
                    0L
                }
                if (state.accessMode == AccessMode.PAID && priceMinor <= 0L) {
                    news(MealPlansNews.ShowSnackbar("Укажите цену больше 0 ₽"))
                    return
                }
                state { copy(isSubmitting = true) }
                commands(
                    MealPlansCommand.CreatePlan(
                        authorId = state.currentUserId,
                        isCoach = state.isCoach,
                        title = title,
                        description = state.descriptionInput.trim(),
                        durationDays = state.durationDays,
                        accessMode = state.accessMode,
                        priceMinor = priceMinor,
                        visibility = state.visibility,
                        starterRecipe = state.recipes.firstOrNull(),
                    ),
                )
            }

            is MealPlansEvent.Ui.PublishPlanClicked -> {
                val plan = state.plans.firstOrNull { it.id == event.planId } ?: return
                if (state.isSubmitting) return
                state { copy(isSubmitting = true) }
                commands(MealPlansCommand.PublishPlan(plan))
            }

            is MealPlansEvent.Ui.OpenPlanDetailsClicked -> {
                commands(MealPlansCommand.LoadPlanDetails(event.planId))
            }

            is MealPlansEvent.Ui.SubscribeToPlanClicked -> {
                if (state.isSubmitting) return
                state { copy(isSubmitting = true) }
                commands(MealPlansCommand.SubscribeToPlan(state.currentUserId, event.planId))
            }

            is MealPlansEvent.Ui.ApplyPlanDayClicked -> {
                val plan = state.plans.firstOrNull { it.id == event.planId }
                    ?: state.selectedPlan?.takeIf { it.id == event.planId }
                    ?: return
                commands(MealPlansCommand.ApplyPlanDay(plan, event.dayNumber))
            }

            is MealPlansEvent.Ui.PublishRecipeClicked -> {
                commands(MealPlansCommand.PublishRecipe(event.recipeId))
            }

            is MealPlansEvent.Ui.ForkRecipeClicked -> {
                commands(MealPlansCommand.ForkRecipe(event.recipeId))
            }

            is MealPlansEvent.Ui.LogRecipeToDiaryClicked -> {
                val recipe = state.recipes.firstOrNull { it.id == event.recipeId } ?: return
                commands(MealPlansCommand.LogRecipeToDiary(recipe))
            }

            MealPlansEvent.Ui.CreateQuickRecipeClicked -> {
                if (state.isSubmitting) return
                state { copy(isSubmitting = true) }
                commands(MealPlansCommand.CreateQuickRecipe(state.currentUserId, state.isCoach))
            }

            is MealPlansEvent.Internal.DataLoaded -> {
                state {
                    copy(
                        plans = event.plans,
                        selectedPlan = event.selectedPlan ?: state.selectedPlan,
                        recipes = event.recipes,
                        isSubmitting = false,
                        content = MealPlansState.Content.Loaded,
                    )
                }
            }

            is MealPlansEvent.Internal.LoadFailed -> {
                state {
                    copy(
                        isSubmitting = false,
                        content = MealPlansState.Content.Error(
                            event.error.message ?: "Не удалось загрузить планы и рецепты",
                        ),
                    )
                }
            }

            is MealPlansEvent.Internal.PlanSaved -> {
                val updated = listOf(event.plan) + state.plans.filterNot { it.id == event.plan.id }
                state {
                    copy(
                        titleInput = "",
                        descriptionInput = "",
                        isSubmitting = false,
                        plans = updated,
                        selectedPlan = event.plan,
                        content = MealPlansState.Content.Loaded,
                    )
                }
                news(MealPlansNews.ShowSnackbar(event.message))
            }

            is MealPlansEvent.Internal.PlanDetailsLoaded -> {
                state {
                    copy(
                        selectedPlan = event.plan,
                        plans = plans.map { if (it.id == event.plan.id) event.plan else it },
                    )
                }
            }

            is MealPlansEvent.Internal.RecipeUpdated -> {
                val updated = listOf(event.recipe) + state.recipes.filterNot { it.id == event.recipe.id }
                state {
                    copy(
                        isSubmitting = false,
                        recipes = updated,
                        content = MealPlansState.Content.Loaded,
                    )
                }
                news(MealPlansNews.ShowSnackbar(event.message))
            }

            is MealPlansEvent.Internal.ActionSucceeded -> {
                state { copy(isSubmitting = false) }
                news(MealPlansNews.ShowSnackbar(event.message))
            }

            is MealPlansEvent.Internal.ActionFailed -> {
                state { copy(isSubmitting = false) }
                news(MealPlansNews.ShowSnackbar(event.error.message ?: "Операция завершилась с ошибкой"))
            }
        }
    }
}
