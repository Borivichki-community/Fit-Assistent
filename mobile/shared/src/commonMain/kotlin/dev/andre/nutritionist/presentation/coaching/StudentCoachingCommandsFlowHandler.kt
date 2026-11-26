package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.repository.CoachingRepository
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class StudentCoachingCommandsFlowHandler(
    private val coachingRepository: CoachingRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<StudentCoachingCommand, StudentCoachingEvent> {

    override fun handle(commands: Flow<StudentCoachingCommand>): Flow<StudentCoachingEvent> = flow {
        commands.collect { command ->
            when (command) {
                is StudentCoachingCommand.LoadData -> {
                    val linksResult = withContext(dispatchers.io) {
                        coachingRepository.getCoachLinks()
                    }
                    linksResult.fold(
                        onSuccess = { allLinks ->
                            val studentLinks = allLinks.filter {
                                command.studentUserId.raw.isBlank() || it.studentId == command.studentUserId
                            }
                            val plansResult = withContext(dispatchers.io) {
                                mealPlanRepository.searchMealPlans(MealPlanSearchCriteria(pageSize = 20))
                            }
                            val plans = plansResult.getOrNull()?.items.orEmpty()
                            emit(StudentCoachingEvent.Internal.DataLoaded(links = studentLinks, plans = plans))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(StudentCoachingEvent.Internal.LoadFailed(error))
                        },
                    )
                }

                is StudentCoachingCommand.RespondInvite -> {
                    val result = withContext(dispatchers.io) {
                        coachingRepository.respondCoachInvite(
                            linkId = command.linkId,
                            accept = command.accept,
                        )
                    }
                    result.fold(
                        onSuccess = { updated -> emit(StudentCoachingEvent.Internal.InviteResponded(updated)) },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(StudentCoachingEvent.Internal.RespondFailed(command.linkId, error))
                        },
                    )
                }

                is StudentCoachingCommand.OpenMealPlan -> {
                    val result = withContext(dispatchers.io) {
                        mealPlanRepository.getMealPlanById(command.planId)
                    }
                    result.fold(
                        onSuccess = { plan -> emit(StudentCoachingEvent.Internal.PlanVerified(plan.id)) },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(StudentCoachingEvent.Internal.PlanOpenFailed(error))
                        },
                    )
                }
            }
        }
    }
}
