package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.calculator.DailyNutritionBalanceCalculator
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.ProfileRepository
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt

class DiaryCommandsFlowHandler(
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers,
    private val profileRepository: ProfileRepository? = null,
    private val userIdProvider: () -> UserId? = { null },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : CommandsFlowHandler<DiaryCommand, DiaryEvent> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun handle(commands: Flow<DiaryCommand>): Flow<DiaryEvent> = channelFlow {
        val observeChannel = Channel<DiaryCommand.ObserveDate>(Channel.UNLIMITED)
        val actionChannel = Channel<DiaryCommand>(Channel.UNLIMITED)

        launch {
            commands.collect { command ->
                when (command) {
                    is DiaryCommand.ObserveDate -> observeChannel.send(command)
                    is DiaryCommand.DeleteRecord,
                    DiaryCommand.SyncPending -> actionChannel.send(command)
                }
            }
            observeChannel.close()
            actionChannel.close()
        }

        launch {
            observeChannel.receiveAsFlow()
                .flatMapLatest { cmd -> observeDate(cmd.dateEpochMs) }
                .collect { send(it) }
        }

        launch {
            actionChannel.receiveAsFlow()
                .flatMapMerge { command ->
                    when (command) {
                        is DiaryCommand.DeleteRecord -> executeDelete(command)
                        DiaryCommand.SyncPending -> executeSync()
                        is DiaryCommand.ObserveDate -> flow { }
                    }
                }
                .collect { send(it) }
        }
    }

    private fun observeDate(dateEpochMs: Long): Flow<DiaryEvent> {
        val (fromEpochMs, toEpochMs) = DateFormatter.dayRangeEpochMs(dateEpochMs, timeZone)
        val recordsFlow = diaryRepository.observeDailyRecords(fromEpochMs, toEpochMs)
        val userId = userIdProvider()

        val combinedFlow: Flow<DiaryEvent> = if (profileRepository != null && userId != null) {
            combine(recordsFlow, profileRepository.observeProfile(userId)) { records, profile ->
                val effectiveGoal = profile?.nutritionGoal?.takeIf { it.targetCalories > 0 } ?: DEFAULT_GOAL
                buildLoadedEvent(records = records, goal = effectiveGoal)
            }
        } else {
            recordsFlow.map { records ->
                buildLoadedEvent(records = records, goal = DEFAULT_GOAL)
            }
        }

        return combinedFlow
            .catch { error ->
                if (error is CancellationException) throw error
                emit(DiaryEvent.Internal.Failed(error))
            }
            .flowOn(dispatchers.io)
    }

    private fun executeDelete(command: DiaryCommand.DeleteRecord): Flow<DiaryEvent> = flow {
        diaryRepository.deleteEntry(command.id)
            .onFailure { error ->
                if (error is CancellationException) throw error
                emit(DiaryEvent.Internal.Failed(error))
            }
    }.flowOn(dispatchers.io)

    private fun executeSync(): Flow<DiaryEvent> = flow {
        diaryRepository.syncPendingRecords()
            .onSuccess { syncedCount ->
                emit(DiaryEvent.Internal.SyncFinished(syncedCount))
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                emit(DiaryEvent.Internal.Failed(error))
            }
    }.flowOn(dispatchers.io)

    private fun buildLoadedEvent(
        records: List<MealRecord>,
        goal: NutritionGoal,
    ): DiaryEvent.Internal.RecordsLoaded {
        val consumed = ConsumedNutrition(
            calories = records.sumOf { it.nutrition.calories }.roundToInt(),
            proteinsGrams = records.sumOf { it.nutrition.proteins },
            fatsGrams = records.sumOf { it.nutrition.fats },
            carbsGrams = records.sumOf { it.nutrition.carbs },
        )
        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = goal,
            consumed = consumed,
        )
        return DiaryEvent.Internal.RecordsLoaded(
            records = records,
            balance = balance,
        )
    }

    companion object {
        val DEFAULT_GOAL = NutritionGoal(
            targetCalories = 2000,
            targetProteinsGrams = 120.0,
            targetFatsGrams = 70.0,
            targetCarbsGrams = 250.0,
        )
    }
}
