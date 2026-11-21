package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Асинхронный обработчик команд главного экрана (Dashboard).
 * Инкапсулирует вызовы репозиториев профиля и дневника, преобразуя
 * результаты в события Internal.Loaded / LoadFailed.
 */
class DashboardCommandsFlowHandler(
    private val diaryRepository: DiaryRepository,
    private val profileRepository: ProfileRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<DashboardCommand, DashboardEvent> {

    override fun handle(commands: Flow<DashboardCommand>): Flow<DashboardEvent> = flow {
        commands.collect { command ->
            when (command) {
                is DashboardCommand.Load -> {
                    val event = withContext(dispatchers.io) {
                        runCatching {
                            val profile = profileRepository.getProfile(command.userId)
                            val diaryResult = diaryRepository.getDailyDiary(command.userId, command.date)
                            val diary = diaryResult.getOrThrow()
                            DashboardEvent.Internal.Loaded(
                                profile = profile,
                                diary = diary,
                            )
                        }.getOrElse { error ->
                            DashboardEvent.Internal.LoadFailed(error)
                        }
                    }
                    emit(event)
                }

                is DashboardCommand.AddMeal -> {
                    val event = withContext(dispatchers.io) {
                        diaryRepository.addEntry(command.entry).fold(
                            onSuccess = { DashboardEvent.Internal.MealAdded(it) },
                            onFailure = { DashboardEvent.Internal.MutationFailed(it) },
                        )
                    }
                    emit(event)
                }

                is DashboardCommand.DeleteMeal -> {
                    val event = withContext(dispatchers.io) {
                        diaryRepository.deleteEntry(command.entryId).fold(
                            onSuccess = { DashboardEvent.Internal.MealDeleted(command.entryId) },
                            onFailure = { DashboardEvent.Internal.MutationFailed(it) },
                        )
                    }
                    emit(event)
                }
            }
        }
    }
}
