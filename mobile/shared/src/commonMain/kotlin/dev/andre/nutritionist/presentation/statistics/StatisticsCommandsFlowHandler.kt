package dev.andre.nutritionist.presentation.statistics

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Асинхронный обработчик команд экрана аналитики питания и бюджета.
 */
class StatisticsCommandsFlowHandler(
    private val analyticsRepository: AnalyticsRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<StatisticsCommand, StatisticsEvent> {

    override fun handle(commands: Flow<StatisticsCommand>): Flow<StatisticsEvent> = flow {
        commands.collect { command ->
            when (command) {
                is StatisticsCommand.LoadAnalytics -> {
                    val result = withContext(dispatchers.io) {
                        analyticsRepository.getNutritionAnalytics(command.userId, command.period).fold(
                            onSuccess = { StatisticsEvent.Internal.AnalyticsLoaded(it) },
                            onFailure = { StatisticsEvent.Internal.AnalyticsLoadFailed(it) },
                        )
                    }
                    emit(result)
                }
            }
        }
    }
}
