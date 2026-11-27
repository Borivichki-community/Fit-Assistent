package dev.andre.nutritionist.presentation.dashboard

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.data.local.DiaryInitialSeeder
import dev.andre.nutritionist.domain.calculator.DailyNutritionBalanceCalculator
import dev.andre.nutritionist.domain.calculator.NutritionCalculator
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.ConsumedNutrition
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.NutritionGoal
import dev.andre.nutritionist.domain.model.Profile
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

class DiaryCommandsFlowHandler(
    private val diaryRepository: DiaryRepository,
    private val dispatchers: CoroutineDispatchers,
    private val profileRepository: ProfileRepository? = null,
    private val userIdProvider: () -> UserId? = { null },
    private val diaryInitialSeeder: DiaryInitialSeeder? = null,
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
                .flatMapLatest { cmd -> observeDate(cmd.dateEpochMs, cmd.userId) }
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

    private fun observeDate(
        dateEpochMs: Long,
        commandUserId: UserId?,
    ): Flow<DiaryEvent> {
        val (dayStart, dayEnd) = DateFormatter.dayRangeEpochMs(dateEpochMs, timeZone)
        val (weekStart, weekEnd) = DateFormatter.weekRangeEpochMs(dateEpochMs, timeZone)
        val effectiveUserId = commandUserId ?: userIdProvider()

        val weekRecordsFlow = diaryRepository.observeDailyRecords(weekStart, weekEnd)
            .onStart {
                diaryInitialSeeder?.ensureSeeded()
            }

        val combinedFlow: Flow<DiaryEvent> = if (profileRepository != null && effectiveUserId != null) {
            channelFlow {
                launch {
                    // Подтягиваем профиль с бэкенда в фоне, не блокируя показ локального дневника из Room
                    runCatching { profileRepository.getProfile(effectiveUserId) }
                        .onFailure { if (it is CancellationException) throw it }
                }
                combine(
                    weekRecordsFlow,
                    profileRepository.observeProfile(effectiveUserId),
                ) { weekRecords, profile ->
                    val dayRecords = weekRecords.filter {
                        it.recordedAt.toEpochMilliseconds() in dayStart..dayEnd
                    }
                    buildLoadedEvent(
                        dayRecords = dayRecords,
                        weekRecords = weekRecords,
                        profile = profile,
                    )
                }.collect { send(it) }
            }
        } else {
            weekRecordsFlow.map { weekRecords ->
                val dayRecords = weekRecords.filter {
                    it.recordedAt.toEpochMilliseconds() in dayStart..dayEnd
                }
                buildLoadedEvent(
                    dayRecords = dayRecords,
                    weekRecords = weekRecords,
                    profile = null,
                )
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
        dayRecords: List<MealRecord>,
        weekRecords: List<MealRecord>,
        profile: Profile?,
    ): DiaryEvent.Internal.RecordsLoaded {
        val effectiveGoal = profile?.nutritionGoal?.takeIf { it.targetCalories > 0 } ?: DEFAULT_GOAL
        val consumed = ConsumedNutrition(
            calories = dayRecords.sumOf { it.nutrition.calories }.roundToInt(),
            proteinsGrams = dayRecords.sumOf { it.nutrition.proteins },
            fatsGrams = dayRecords.sumOf { it.nutrition.fats },
            carbsGrams = dayRecords.sumOf { it.nutrition.carbs },
        )
        val balance = DailyNutritionBalanceCalculator.calculate(
            goal = effectiveGoal,
            consumed = consumed,
        )
        val totalCost = NutritionCalculator.sumMealCost(dayRecords)
        val loggedDates = weekRecords.mapTo(HashSet()) { record ->
            record.recordedAt.toLocalDateTime(timeZone).date
        }
        val showBanner = profile == null || profile.heightCm <= 0 || profile.weightKg <= 0.0

        return DiaryEvent.Internal.RecordsLoaded(
            records = dayRecords,
            balance = balance,
            totalCost = totalCost,
            weeklyBudget = profile?.weeklyBudget ?: 0.0,
            targetCalories = effectiveGoal.targetCalories,
            loggedDatesInWeek = loggedDates,
            showCompleteProfileBanner = showBanner,
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
