package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.domain.model.UserId

/**
 * Чистый синхронный редьюсер для экрана управления рационом питания.
 */
class MealPlanManagerUpdate : DslUpdate<MealPlanManagerState, MealPlanManagerEvent, MealPlanManagerCommand, MealPlanManagerNews>() {

    override fun NextBuilder<MealPlanManagerState, MealPlanManagerCommand, MealPlanManagerNews>.update(
        event: MealPlanManagerEvent,
    ) {
        when (event) {
            is MealPlanManagerEvent.Ui.Load -> {
                state { copy(content = MealPlanManagerState.Content.Loading, userId = event.userId) }
                commands(MealPlanManagerCommand.Load(event.userId))
            }
            MealPlanManagerEvent.Ui.Retry -> {
                val uid = state.userId ?: UserId("testuser")
                state { copy(content = MealPlanManagerState.Content.Loading, userId = uid) }
                commands(MealPlanManagerCommand.Load(uid))
            }
            is MealPlanManagerEvent.Ui.SelectTab -> {
                val currentContent = state.content
                if (currentContent is MealPlanManagerState.Content.Success) {
                    state {
                        copy(
                            selectedTab = event.tab,
                            content = currentContent.copy(selectedTab = event.tab),
                        )
                    }
                } else {
                    state { copy(selectedTab = event.tab) }
                }
            }
            is MealPlanManagerEvent.Ui.SelectDay -> {
                val currentContent = state.content
                if (currentContent is MealPlanManagerState.Content.Success) {
                    state {
                        copy(
                            content = currentContent.copy(selectedDayIndex = event.dayIndex),
                        )
                    }
                }
            }
            is MealPlanManagerEvent.Ui.ToggleGrocery -> {
                val currentContent = state.content
                if (currentContent is MealPlanManagerState.Content.Success) {
                    commands(
                        MealPlanManagerCommand.ToggleGrocery(
                            planId = currentContent.activePlan.id,
                            itemId = event.itemId,
                        ),
                    )
                }
            }
            MealPlanManagerEvent.Ui.DeactivatePlan -> {
                val uid = state.userId ?: UserId("testuser")
                commands(MealPlanManagerCommand.DeactivatePlan(uid))
            }
            MealPlanManagerEvent.Ui.NavigateToCatalog -> {
                news(MealPlanManagerNews.NavigateToCatalog)
            }
            is MealPlanManagerEvent.Internal.Loaded -> {
                val plan = event.activePlan
                if (plan != null) {
                    state {
                        copy(
                            content = MealPlanManagerState.Content.Success(
                                activePlan = plan,
                                days = event.days,
                                groceryList = event.groceryList,
                                selectedDayIndex = 0,
                                selectedTab = state.selectedTab,
                            ),
                        )
                    }
                } else {
                    state {
                        copy(
                            content = MealPlanManagerState.Content.Empty(
                                message = "У вас пока нет активного плана питания. Выберите сбалансированный рацион в каталоге или сгенерируйте его с помощью ИИ.",
                            ),
                        )
                    }
                }
            }
            is MealPlanManagerEvent.Internal.GroceryToggled -> {
                val currentContent = state.content
                if (currentContent is MealPlanManagerState.Content.Success) {
                    val updated = currentContent.groceryList.map { item ->
                        if (item.id == event.itemId) {
                            item.copy(isBought = event.isBought)
                        } else {
                            item
                        }
                    }
                    state {
                        copy(
                            content = currentContent.copy(groceryList = updated),
                        )
                    }
                }
            }
            MealPlanManagerEvent.Internal.PlanDeactivated -> {
                state {
                    copy(
                        content = MealPlanManagerState.Content.Empty(
                            message = "План питания деактивирован. Вы можете выбрать новый в каталоге.",
                        ),
                    )
                }
                news(MealPlanManagerNews.ShowSnackbar("План питания успешно отключен"))
            }
            is MealPlanManagerEvent.Internal.Failed -> {
                val message = event.error.message ?: "Не удалось загрузить план питания"
                state { copy(content = MealPlanManagerState.Content.Error(message)) }
            }
        }
    }
}
