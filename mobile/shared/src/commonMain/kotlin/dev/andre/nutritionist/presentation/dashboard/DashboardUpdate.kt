package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.DslUpdate

/**
 * Чистый синхронный редьюсер главного экрана (Dashboard).
 * Строго изолирован от сайд-эффектов, корутин и сетевых вызовов.
 * Реализует исчерпывающий анализ всех событий без ветки else.
 */
class DashboardUpdate : DslUpdate<DashboardState, DashboardEvent, DashboardCommand, DashboardNews>() {

    override fun NextBuilder<DashboardState, DashboardCommand, DashboardNews>.update(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Ui.LoadDashboard -> {
                state {
                    copy(
                        content = DashboardState.Content.Loading,
                        currentUserId = event.userId,
                        selectedDate = event.date,
                        showCompleteProfileBanner = event.showCompleteProfileBanner,
                    )
                }
                commands(DashboardCommand.Load(event.userId, event.date))
            }

            is DashboardEvent.Ui.SelectDate -> {
                val userId = state.currentUserId
                state {
                    copy(
                        selectedDate = event.date,
                        content = DashboardState.Content.Loading,
                    )
                }
                if (userId != null) {
                    commands(DashboardCommand.Load(userId, event.date))
                }
            }

            is DashboardEvent.Ui.ToggleMealSection -> {
                val currentExpanded = state.expandedMeals
                val updatedExpanded = if (currentExpanded.contains(event.mealType)) {
                    currentExpanded - event.mealType
                } else {
                    currentExpanded + event.mealType
                }
                state {
                    val updatedContent = when (val c = content) {
                        is DashboardState.Content.Success -> c.copy(expandedMeals = updatedExpanded)
                        DashboardState.Content.Loading -> c
                        is DashboardState.Content.Error -> c
                    }
                    copy(
                        expandedMeals = updatedExpanded,
                        content = updatedContent,
                    )
                }
            }

            is DashboardEvent.Ui.AddMealOptimistic -> {
                val content = state.content
                if (content is DashboardState.Content.Success) {
                    val currentDiary = content.diary
                    val updatedEntries = currentDiary.entries + event.entry
                    state {
                        copy(content = content.copy(diary = currentDiary.copy(entries = updatedEntries)))
                    }
                }
                commands(DashboardCommand.AddMeal(event.entry))
            }

            is DashboardEvent.Ui.DeleteMeal -> {
                val userId = state.currentUserId
                val date = state.selectedDate
                val content = state.content
                if (content is DashboardState.Content.Success) {
                    val currentDiary = content.diary
                    val updatedEntries = currentDiary.entries.filterNot { it.id == event.entryId }
                    state {
                        copy(content = content.copy(diary = currentDiary.copy(entries = updatedEntries)))
                    }
                }
                if (userId != null && date != null) {
                    commands(DashboardCommand.DeleteMeal(event.entryId, userId, date))
                }
            }

            is DashboardEvent.Ui.Retry -> {
                val userId = state.currentUserId
                val date = state.selectedDate
                if (userId != null && date != null) {
                    state { copy(content = DashboardState.Content.Loading) }
                    commands(DashboardCommand.Load(userId, date))
                }
            }

            is DashboardEvent.Internal.Loaded -> {
                state {
                    copy(
                        content = DashboardState.Content.Success(
                            profile = event.profile,
                            diary = event.diary,
                            expandedMeals = expandedMeals,
                            showCompleteProfileBanner = showCompleteProfileBanner,
                        ),
                    )
                }
            }

            is DashboardEvent.Internal.LoadFailed -> {
                state {
                    copy(
                        content = DashboardState.Content.Error(
                            message = event.error.message ?: "Ошибка загрузки данных",
                        ),
                    )
                }
            }

            is DashboardEvent.Internal.MealAdded -> {
                // Оптимистичное добавление уже отражено в состоянии
            }

            is DashboardEvent.Internal.MealDeleted -> {
                // Удаление уже отражено в состоянии
            }

            is DashboardEvent.Internal.MutationFailed -> {
                news(DashboardNews.ShowSnackbar(event.error.message ?: "Ошибка при сохранении данных"))
            }
        }
    }
}
