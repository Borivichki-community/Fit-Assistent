package dev.andre.nutritionist.presentation.planmanager

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Асинхронный обработчик команд экрана управления рационом питания.
 */
class MealPlanManagerCommandsFlowHandler(
    private val mealPlanRepository: MealPlanRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<MealPlanManagerCommand, MealPlanManagerEvent> {

    override fun handle(commands: Flow<MealPlanManagerCommand>): Flow<MealPlanManagerEvent> = flow {
        commands.collect { command ->
            when (command) {
                is MealPlanManagerCommand.Load -> {
                    val event = withContext(dispatchers.io) {
                        runCatching {
                            val activePlan = mealPlanRepository.getActiveMealPlan(command.userId).getOrThrow()
                            if (activePlan != null) {
                                val days = mealPlanRepository.getMealPlanDays(activePlan.id).getOrThrow()
                                val groceries = mealPlanRepository.getGroceryList(activePlan.id).getOrThrow()
                                MealPlanManagerEvent.Internal.Loaded(
                                    activePlan = activePlan,
                                    days = days,
                                    groceryList = groceries,
                                )
                            } else {
                                MealPlanManagerEvent.Internal.Loaded(
                                    activePlan = null,
                                    days = emptyList(),
                                    groceryList = emptyList(),
                                )
                            }
                        }.fold(
                            onSuccess = { it },
                            onFailure = { MealPlanManagerEvent.Internal.Failed(it) },
                        )
                    }
                    emit(event)
                }

                is MealPlanManagerCommand.ToggleGrocery -> {
                    val event = withContext(dispatchers.io) {
                        mealPlanRepository.toggleGroceryItem(command.planId, command.itemId).fold(
                            onSuccess = { isBought ->
                                MealPlanManagerEvent.Internal.GroceryToggled(
                                    itemId = command.itemId,
                                    isBought = isBought,
                                )
                            },
                            onFailure = { MealPlanManagerEvent.Internal.Failed(it) },
                        )
                    }
                    emit(event)
                }

                is MealPlanManagerCommand.DeactivatePlan -> {
                    val event = withContext(dispatchers.io) {
                        mealPlanRepository.deactivateMealPlan(command.userId).fold(
                            onSuccess = { MealPlanManagerEvent.Internal.PlanDeactivated },
                            onFailure = { MealPlanManagerEvent.Internal.Failed(it) },
                        )
                    }
                    emit(event)
                }
            }
        }
    }
}
