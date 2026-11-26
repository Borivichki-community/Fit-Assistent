package dev.andre.nutritionist.presentation.mappers

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.MealRecordUiModel
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt

object MealRecordUiMapper {

    fun MealRecord.toUiModel(
        targetCalories: Int = 2000,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): MealRecordUiModel {
        val proteins = NutritionFormatter.formatGram(nutrition.proteins)
        val fats = NutritionFormatter.formatGram(nutrition.fats)
        val carbs = NutritionFormatter.formatGram(nutrition.carbs)

        val proteinsCompact = "${NutritionFormatter.formatMacroValue(nutrition.proteins)}г"
        val fatsCompact = "${NutritionFormatter.formatMacroValue(nutrition.fats)}г"
        val carbsCompact = "${NutritionFormatter.formatMacroValue(nutrition.carbs)}г"

        val shareRatio = if (targetCalories > 0) {
            nutrition.calories / targetCalories.toDouble()
        } else {
            0.0
        }

        return MealRecordUiModel(
            id = id,
            mealType = mealType,
            mealTypeLabel = mealType.toLabel(),
            title = nameSnapshot,
            weightLabel = NutritionFormatter.formatGram(weightGrams),
            caloriesLabel = NutritionFormatter.formatCalories(nutrition.calories),
            caloriesInt = nutrition.calories.roundToInt(),
            proteinsLabel = proteins,
            fatsLabel = fats,
            carbsLabel = carbs,
            macrosLabel = "Б $proteins • Ж $fats • У $carbs",
            compactMacrosLabel = "Б: $proteinsCompact • Ж: $fatsCompact • У: $carbsCompact",
            costLabel = cost?.let { MoneyFormatter.format(it) },
            dailyBalanceShareLabel = "${NutritionFormatter.formatPercentage(shareRatio)} от суточной нормы",
            durationLabel = "1 мин",
            syncStatus = syncStatus,
            syncStatusLabel = syncStatus.toLabel(),
            timeLabel = DateFormatter.formatTime(recordedAt.toEpochMilliseconds(), timeZone),
        )
    }

    private fun MealType.toLabel(): String = when (this) {
        MealType.BREAKFAST -> "Завтрак"
        MealType.LUNCH -> "Обед"
        MealType.DINNER -> "Ужин"
        MealType.SNACK -> "Перекус"
    }

    private fun SyncStatus.toLabel(): String = when (this) {
        SyncStatus.PENDING -> "Ожидает синхронизации"
        SyncStatus.SYNCED -> "Синхронизировано"
        SyncStatus.FAILED -> "Ошибка синхронизации"
        SyncStatus.DELETED_PENDING -> "Ожидает удаления"
    }
}
