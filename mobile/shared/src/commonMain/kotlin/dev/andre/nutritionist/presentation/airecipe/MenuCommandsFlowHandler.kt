package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuUseCase
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn

class MenuCommandsFlowHandler(
    private val generateBudgetMenuUseCase: GenerateBudgetMenuUseCase,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<MenuCommand, MenuEvent> {

    override fun handle(commands: Flow<MenuCommand>): Flow<MenuEvent> = channelFlow {
        commands.collectLatest { command ->
            when (command) {
                is MenuCommand.GenerateMenu -> {
                    val result = generateBudgetMenuUseCase(command.params)
                    result.onSuccess { progressFlow ->
                        progressFlow
                            .flowOn(dispatchers.io)
                            .catch { error ->
                                if (error is CancellationException) throw error
                                send(MenuEvent.Internal.GenerationFailed(error))
                            }
                            .collect { progress ->
                                when (progress) {
                                    MenuGenerationProgress.Initializing,
                                    is MenuGenerationProgress.StreamingDay -> {
                                        send(MenuEvent.Internal.ProgressUpdated(progress))
                                    }
                                    is MenuGenerationProgress.Completed -> {
                                        send(MenuEvent.Internal.GenerationCompleted(progress.plan))
                                    }
                                }
                            }
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        send(MenuEvent.Internal.GenerationFailed(error))
                    }
                }
                MenuCommand.CancelGeneration -> Unit
            }
        }
    }
}
