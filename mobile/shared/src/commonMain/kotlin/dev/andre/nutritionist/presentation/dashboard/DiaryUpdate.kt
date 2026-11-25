package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.DslUpdate
import dev.andre.nutritionist.presentation.mappers.DailyNutritionUiMapper.toUiModel
import dev.andre.nutritionist.presentation.mappers.MealRecordUiMapper.toUiModel

class DiaryUpdate : DslUpdate<DiaryState, DiaryEvent, DiaryCommand, DiaryNews>() {

    override fun NextBuilder<DiaryState, DiaryCommand, DiaryNews>.update(event: DiaryEvent) {
        when (event) {
            is DiaryEvent.Ui.DateSelected -> {
                state {
                    copy(
                        selectedDateEpochMs = event.dateEpochMs,
                        content = DiaryState.Content.Loading,
                    )
                }
                commands(DiaryCommand.ObserveDate(event.dateEpochMs))
            }

            is DiaryEvent.Ui.DeleteRecord -> {
                commands(DiaryCommand.DeleteRecord(event.id))
            }

            DiaryEvent.Ui.ManualSyncClicked -> {
                commands(DiaryCommand.SyncPending)
            }

            is DiaryEvent.Internal.RecordsLoaded -> {
                val uiItems = event.records.map { it.toUiModel() }
                val summaryUi = event.balance.toUiModel()
                state {
                    copy(
                        items = uiItems,
                        dailySummary = summaryUi,
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
}
