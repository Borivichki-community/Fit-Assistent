package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.core.kotea.DslUpdate

/**
 * Чистая MVI-редукция экрана аналитики питания и бюджета.
 * Не содержит побочных эффектов и веток else.
 */
class StatisticsUpdate : DslUpdate<StatisticsState, StatisticsEvent, StatisticsCommand, StatisticsNews>() {

    override fun NextBuilder<StatisticsState, StatisticsCommand, StatisticsNews>.update(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.Ui -> handleUiEvent(event)
            is StatisticsEvent.Internal -> handleInternalEvent(event)
        }
    }

    private fun NextBuilder<StatisticsState, StatisticsCommand, StatisticsNews>.handleUiEvent(event: StatisticsEvent.Ui) {
        when (event) {
            is StatisticsEvent.Ui.Init -> {
                state { copy(userId = event.userId, content = StatisticsState.Content.Loading) }
                commands(StatisticsCommand.LoadAnalytics(userId = event.userId, period = state.period))
            }

            is StatisticsEvent.Ui.PeriodSelected -> {
                if (state.period != event.period) {
                    state { copy(period = event.period, content = StatisticsState.Content.Loading) }
                    commands(StatisticsCommand.LoadAnalytics(userId = state.userId, period = event.period))
                }
            }

            is StatisticsEvent.Ui.RetryClicked -> {
                state { copy(content = StatisticsState.Content.Loading) }
                commands(StatisticsCommand.LoadAnalytics(userId = state.userId, period = state.period))
            }
        }
    }

    private fun NextBuilder<StatisticsState, StatisticsCommand, StatisticsNews>.handleInternalEvent(event: StatisticsEvent.Internal) {
        when (event) {
            is StatisticsEvent.Internal.AnalyticsLoaded -> {
                state { copy(content = StatisticsState.Content.Success(event.analytics)) }
            }

            is StatisticsEvent.Internal.AnalyticsLoadFailed -> {
                state {
                    copy(
                        content = StatisticsState.Content.Error(
                            event.throwable.message ?: "Не удалось загрузить аналитику",
                        ),
                    )
                }
            }
        }
    }
}
