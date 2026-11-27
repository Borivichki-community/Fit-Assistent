package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.GrantTargetType
import dev.andre.nutritionist.domain.model.MealPlanSearchCriteria
import dev.andre.nutritionist.domain.repository.CoachingRepository
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.MealPlanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class CoachStudentMenuCommandsFlowHandler(
    private val coachingRepository: CoachingRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers,
    private val nowProvider: () -> Instant = { Clock.System.now() },
) : CommandsFlowHandler<CoachStudentMenuCommand, CoachStudentMenuEvent> {

    override fun handle(commands: Flow<CoachStudentMenuCommand>): Flow<CoachStudentMenuEvent> = flow {
        commands.collect { command ->
            when (command) {
                is CoachStudentMenuCommand.LoadAll -> {
                    val event = try {
                        coroutineScope {
                            val plansDeferred = async(dispatchers.io) {
                                mealPlanRepository.searchMealPlans(MealPlanSearchCriteria(pageSize = 30))
                            }
                            val selectedPlanDeferred = command.planId?.let { id ->
                                async(dispatchers.io) { mealPlanRepository.getMealPlanById(id) }
                            }
                            val diaryDeferred = async(dispatchers.io) {
                                loadDiaryForDate(command.studentId, command.dateEpochMs)
                            }
                            val plansResult = plansDeferred.await()
                            val selectedPlan = selectedPlanDeferred?.await()?.getOrNull()
                            val diaryResult = diaryDeferred.await()
                            val plans = plansResult.getOrNull()?.items.orEmpty()
                            val diaryRecords = diaryResult.getOrNull().orEmpty()
                            val diaryError = diaryResult.exceptionOrNull()?.let { err ->
                                if (err is CancellationException) throw err
                                err.message ?: "Нет доступа к дневнику ученика"
                            }
                            if (plansResult.isFailure && diaryResult.isFailure) {
                                val failure = plansResult.exceptionOrNull()
                                    ?: diaryResult.exceptionOrNull()
                                    ?: IllegalStateException("Ошибка загрузки")
                                if (failure is CancellationException) throw failure
                                CoachStudentMenuEvent.Internal.LoadFailed(failure)
                            } else {
                                CoachStudentMenuEvent.Internal.Loaded(
                                    availablePlans = plans,
                                    selectedPlan = selectedPlan,
                                    diaryRecords = diaryRecords,
                                    diaryError = diaryError,
                                )
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        CoachStudentMenuEvent.Internal.LoadFailed(error)
                    }
                    emit(event)
                }

                is CoachStudentMenuCommand.LoadDiary -> {
                    val result = withContext(dispatchers.io) {
                        loadDiaryForDate(command.studentId, command.dateEpochMs)
                    }
                    result.fold(
                        onSuccess = { records ->
                            emit(CoachStudentMenuEvent.Internal.DiaryLoaded(records = records, error = null))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(
                                CoachStudentMenuEvent.Internal.DiaryLoaded(
                                    records = emptyList(),
                                    error = error.message ?: "Не удалось загрузить дневник ученика",
                                ),
                            )
                        },
                    )
                }

                is CoachStudentMenuCommand.LoadPlan -> {
                    val result = withContext(dispatchers.io) {
                        mealPlanRepository.getMealPlanById(command.planId)
                    }
                    result.fold(
                        onSuccess = { plan -> emit(CoachStudentMenuEvent.Internal.PlanLoaded(plan)) },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(CoachStudentMenuEvent.Internal.PlanLoadFailed(error))
                        },
                    )
                }

                is CoachStudentMenuCommand.GrantAccess -> {
                    val expiresAt = command.expiresInDays?.let { days ->
                        nowProvider() + days.days
                    }
                    val result = withContext(dispatchers.io) {
                        coachingRepository.grantAccess(
                            userId = command.studentId,
                            targetType = GrantTargetType.MEAL_PLAN,
                            targetId = command.planId,
                            grantSource = command.grantSource,
                            expiresAt = expiresAt,
                        )
                    }
                    result.fold(
                        onSuccess = { grant -> emit(CoachStudentMenuEvent.Internal.AccessGranted(grant)) },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(CoachStudentMenuEvent.Internal.GrantFailed(error))
                        },
                    )
                }

                is CoachStudentMenuCommand.RevokeAccess -> {
                    val result = withContext(dispatchers.io) {
                        coachingRepository.revokeAccess(command.grantId)
                    }
                    result.fold(
                        onSuccess = { emit(CoachStudentMenuEvent.Internal.AccessRevoked(command.grantId)) },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(CoachStudentMenuEvent.Internal.RevokeFailed(error))
                        },
                    )
                }
            }
        }
    }

    private suspend fun loadDiaryForDate(
        studentId: dev.andre.nutritionist.domain.model.UserId,
        dateEpochMs: Long,
    ) = Instant.fromEpochMilliseconds(dateEpochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
        .let { isoDate ->
            diaryRepository.getRange(
                fromIso = isoDate,
                untilIso = isoDate,
                targetUserId = studentId,
            )
        }
}
