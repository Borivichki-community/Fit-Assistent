package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import dev.andre.nutritionist.presentation.mappers.DailyNutritionUiMapper.toUiModel
import dev.andre.nutritionist.presentation.mappers.MealRecordUiMapper.toUiModel
import kotlinx.datetime.TimeZone

class DiaryUpdate(
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : DslUpdate<DiaryState, DiaryEvent, DiaryCommand, DiaryNews>() {

    override fun NextBuilder<DiaryState, DiaryCommand, DiaryNews>.update(event: DiaryEvent) {
        when (event) {
            is DiaryEvent.Ui.DateSelected -> {
                val effectiveUserId = event.userId ?: state.currentUserId
                state {
                    copy(
                        selectedDateEpochMs = event.dateEpochMs,
                        currentUserId = effectiveUserId,
                        content = DiaryState.Content.Loading,
                    )
                }
                commands(DiaryCommand.ObserveDate(dateEpochMs = event.dateEpochMs, userId = effectiveUserId))
            }

            DiaryEvent.Ui.PreviousWeekClicked -> {
                val currentEpochMs = state.selectedDateEpochMs
                if (currentEpochMs > 0L) {
                    val shiftedEpochMs = DateFormatter.shiftDaysEpochMs(currentEpochMs, -DAYS_IN_WEEK, timeZone)
                    val userId = state.currentUserId
                    state {
                        copy(
                            selectedDateEpochMs = shiftedEpochMs,
                            content = DiaryState.Content.Loading,
                        )
                    }
                    commands(DiaryCommand.ObserveDate(dateEpochMs = shiftedEpochMs, userId = userId))
                }
            }

            DiaryEvent.Ui.NextWeekClicked -> {
                val currentEpochMs = state.selectedDateEpochMs
                if (currentEpochMs > 0L) {
                    val shiftedEpochMs = DateFormatter.shiftDaysEpochMs(currentEpochMs, DAYS_IN_WEEK, timeZone)
                    val userId = state.currentUserId
                    state {
                        copy(
                            selectedDateEpochMs = shiftedEpochMs,
                            content = DiaryState.Content.Loading,
                        )
                    }
                    commands(DiaryCommand.ObserveDate(dateEpochMs = shiftedEpochMs, userId = userId))
                }
            }

            DiaryEvent.Ui.Retry -> {
                val currentEpochMs = state.selectedDateEpochMs
                if (currentEpochMs > 0L) {
                    val userId = state.currentUserId
                    state { copy(content = DiaryState.Content.Loading) }
                    commands(DiaryCommand.ObserveDate(dateEpochMs = currentEpochMs, userId = userId))
                }
            }

            is DiaryEvent.Ui.ToggleMealSection -> {
                val currentExpanded = state.expandedMeals
                val updated = if (currentExpanded.contains(event.mealType)) {
                    currentExpanded - event.mealType
                } else {
                    currentExpanded + event.mealType
                }
                state { copy(expandedMeals = updated) }
            }

            is DiaryEvent.Ui.DeleteRecord -> {
                commands(DiaryCommand.DeleteRecord(event.id))
            }

            DiaryEvent.Ui.ManualSyncClicked -> {
                commands(DiaryCommand.SyncPending)
            }

            is DiaryEvent.Internal.RecordsLoaded -> {
                val uiItems = event.records.map {
                    it.toUiModel(
                        targetCalories = event.targetCalories,
                        timeZone = timeZone,
                    )
                }
                val summaryUi = event.balance.toUiModel(
                    totalCost = event.totalCost,
                    weeklyBudgetRubles = event.weeklyBudget,
                )
                state {
                    copy(
                        items = uiItems,
                        dailySummary = summaryUi,
                        loggedDatesInWeek = event.loggedDatesInWeek,
                        showCompleteProfileBanner = event.showCompleteProfileBanner,
                        content = if (uiItems.isEmpty()) {
                            DiaryState.Content.Empty
                        } else {
                            DiaryState.Content.Success
                        },
                    )
                }
            }

            is DiaryEvent.Internal.SyncFinished -> {
                val message = if (event.syncedCount > 0) {
                    "Синхронизировано записей: ${event.syncedCount}"
                } else {
                    "Все записи синхронизированы"
                }
                news(DiaryNews.ShowSnackbar(message))
            }

            is DiaryEvent.Internal.Failed -> {
                val message = event.error.message?.takeIf { it.isNotBlank() }
                    ?: "Ошибка при работе с дневником"
                if (state.items.isEmpty()) {
                    state { copy(content = DiaryState.Content.Error(message)) }
                }
                news(DiaryNews.ShowSnackbar(message))
            }
        }
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
