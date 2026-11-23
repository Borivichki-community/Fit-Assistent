package dev.andre.nutritionist.presentation.catalog

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Асинхронный обработчик команд экрана каталога рационов.
 */
class CatalogCommandsFlowHandler(
    private val mealPlanRepository: MealPlanRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<CatalogCommand, CatalogEvent> {

    override fun handle(commands: Flow<CatalogCommand>): Flow<CatalogEvent> = flow {
        commands.collect { command ->
            when (command) {
                is CatalogCommand.LoadPlans -> {
                    val event = withContext(dispatchers.io) {
                        mealPlanRepository.getMealPlans(command.query, command.category).fold(
                            onSuccess = { CatalogEvent.Internal.PlansLoaded(it) },
                            onFailure = { CatalogEvent.Internal.LoadFailed(it) },
                        )
                    }
                    emit(event)
                }

                is CatalogCommand.ToggleFavorite -> {
                    val event = withContext(dispatchers.io) {
                        mealPlanRepository.toggleFavorite(command.planId).fold(
                            onSuccess = { CatalogEvent.Internal.FavoriteUpdated(command.planId, it) },
                            onFailure = { CatalogEvent.Internal.LoadFailed(it) },
                        )
                    }
                    emit(event)
                }
            }
        }
    }
}
