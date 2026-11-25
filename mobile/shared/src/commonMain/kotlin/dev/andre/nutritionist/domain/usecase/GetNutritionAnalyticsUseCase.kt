package dev.andre.nutritionist.domain.usecase

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.DailyNutritionPoint
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.PeriodAnalytics
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.domain.repository.DiaryRepository
import dev.andre.nutritionist.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * UseCase расчёта аналитики КБЖУ и трат бюджета за произвольный период (включая режим тренера через targetUserId).
 */
class GetNutritionAnalyticsUseCase(
    private val diaryRepository: DiaryRepository,
    private val profileRepository: ProfileRepository,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers(),
    private val currentUserIdProvider: () -> UserId? = { null },
) {

    suspend operator fun invoke(
        fromDate: LocalDate,
        toDate: LocalDate,
        targetUserId: UserId? = null,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<PeriodAnalytics> = withContext(dispatchers.io) {
        if (fromDate > toDate) {
            return@withContext Result.failure(
                DomainError.ValidationError(
                    field = "dateRange",
                    reason = "fromDate ($fromDate) must be on or before toDate ($toDate)",
                ),
            )
        }

        try {
            val records = diaryRepository.getRange(
                fromIso = fromDate.toString(),
                untilIso = toDate.toString(),
                targetUserId = targetUserId,
                timeZone = timeZone.id,
            ).getOrThrow()

            val effectiveUserId = targetUserId ?: currentUserIdProvider()
            val profile = effectiveUserId?.let { profileRepository.getProfile(it) }

            val targetCaloriesPerDay = profile?.nutritionGoal?.targetCalories ?: 0
            val targetProteinsPerDay = profile?.nutritionGoal?.targetProteinsGrams ?: 0.0
            val targetFatsPerDay = profile?.nutritionGoal?.targetFatsGrams ?: 0.0
            val targetCarbsPerDay = profile?.nutritionGoal?.targetCarbsGrams ?: 0.0

            val daysCount = fromDate.daysUntil(toDate) + 1
            val recordsByDate = records
                .groupBy { it.recordedAt.toLocalDateTime(timeZone).date }
                .filterKeys { it in fromDate..toDate }

            val dailyBreakdown = buildList(daysCount) {
                var current = fromDate
                while (current <= toDate) {
                    val dayRecords = recordsByDate[current].orEmpty()
                    val dayCalories = dayRecords.sumOf { it.nutrition.calories }.roundToInt()
                    val dayProteins = roundOneDecimal(dayRecords.sumOf { it.nutrition.proteins })
                    val dayFats = roundOneDecimal(dayRecords.sumOf { it.nutrition.fats })
                    val dayCarbs = roundOneDecimal(dayRecords.sumOf { it.nutrition.carbs })
                    val daySpent = Money.fromMinor(dayRecords.sumOf { it.cost?.amountMinor ?: 0L })
                    val isGoalMet = isCalorieGoalMet(
                        dayCalories = dayCalories,
                        hasEntries = dayRecords.isNotEmpty(),
                        targetCalories = targetCaloriesPerDay,
                    )

                    add(
                        DailyNutritionPoint(
                            date = current,
                            calories = dayCalories,
                            proteinsGrams = dayProteins,
                            fatsGrams = dayFats,
                            carbsGrams = dayCarbs,
                            spent = daySpent,
                            isCalorieGoalMet = isGoalMet,
                        ),
                    )
                    current = current.plus(1, DateTimeUnit.DAY)
                }
            }

            val activeDays = dailyBreakdown.filter { recordsByDate.containsKey(it.date) }
            val activeDaysCount = activeDays.size

            val totalCalories = dailyBreakdown.sumOf { it.calories }
            val totalProteins = recordsByDate.values.flatten().sumOf { it.nutrition.proteins }
            val totalFats = recordsByDate.values.flatten().sumOf { it.nutrition.fats }
            val totalCarbs = recordsByDate.values.flatten().sumOf { it.nutrition.carbs }

            val averageCalories = if (activeDaysCount > 0) {
                (totalCalories.toDouble() / activeDaysCount).roundToInt()
            } else {
                0
            }
            val averageProteins = if (activeDaysCount > 0) {
                roundOneDecimal(totalProteins / activeDaysCount)
            } else {
                0.0
            }
            val averageFats = if (activeDaysCount > 0) {
                roundOneDecimal(totalFats / activeDaysCount)
            } else {
                0.0
            }
            val averageCarbs = if (activeDaysCount > 0) {
                roundOneDecimal(totalCarbs / activeDaysCount)
            } else {
                0.0
            }

            val totalSpent = Money.fromMinor(dailyBreakdown.sumOf { it.spent.amountMinor })
            val budgetLimit = if (profile != null && profile.weeklyBudget > 0.0) {
                Money.fromRubles((profile.weeklyBudget / 7.0) * daysCount)
            } else {
                Money.ZERO
            }
            val budgetUsagePercent = if (budgetLimit.amountMinor > 0L) {
                ((totalSpent.amountMinor.toDouble() / budgetLimit.amountMinor.toDouble()) * 100.0).roundToInt()
            } else {
                0
            }
            val isBudgetExceeded = budgetLimit.amountMinor > 0L && totalSpent > budgetLimit

            val calorieAdherencePercent = if (activeDaysCount > 0 && targetCaloriesPerDay > 0) {
                val deviationRatio = abs(averageCalories - targetCaloriesPerDay) / targetCaloriesPerDay.toDouble()
                ((1.0 - deviationRatio) * 100.0).coerceIn(0.0, 100.0).roundToInt()
            } else {
                0
            }

            Result.success(
                PeriodAnalytics(
                    fromDate = fromDate,
                    toDate = toDate,
                    daysCount = daysCount,
                    activeDaysCount = activeDaysCount,
                    totalCalories = totalCalories,
                    averageCalories = averageCalories,
                    averageProteinsGrams = averageProteins,
                    averageFatsGrams = averageFats,
                    averageCarbsGrams = averageCarbs,
                    targetCaloriesPerDay = targetCaloriesPerDay,
                    targetProteinsPerDay = targetProteinsPerDay,
                    targetFatsPerDay = targetFatsPerDay,
                    targetCarbsPerDay = targetCarbsPerDay,
                    totalSpent = totalSpent,
                    budgetLimit = budgetLimit,
                    budgetUsagePercent = budgetUsagePercent,
                    isBudgetExceeded = isBudgetExceeded,
                    calorieAdherencePercent = calorieAdherencePercent,
                    dailyBreakdown = dailyBreakdown,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun isCalorieGoalMet(
        dayCalories: Int,
        hasEntries: Boolean,
        targetCalories: Int,
    ): Boolean {
        if (!hasEntries || targetCalories <= 0) return false
        val tolerance = targetCalories * GOAL_TOLERANCE_RATIO
        return abs(dayCalories - targetCalories) <= tolerance
    }

    private fun roundOneDecimal(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0

    private companion object {
        const val GOAL_TOLERANCE_RATIO = 0.10
    }
}
