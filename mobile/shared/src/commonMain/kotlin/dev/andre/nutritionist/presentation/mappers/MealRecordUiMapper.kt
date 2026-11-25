package dev.andre.nutritionist.presentation.mappers

import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.presentation.formatters.DateFormatter
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.MealRecordUiModel
import kotlinx.datetime.TimeZone

object MealRecordUiMapper {

    fun MealRecord.toUiModel(
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): MealRecordUiModel {
        val proteins = NutritionFormatter.formatGram(nutrition.proteins)
        val fats = NutritionFormatter.formatGram(nutrition.fats)
        val carbs = NutritionFormatter.formatGram(nutrition.carbs)

        return MealRecordUiModel(
            id = id,
            mealType = mealType,
            mealTypeLabel = mealType.toLabel(),
            title = nameSnapshot,
            weightLabel = NutritionFormatter.formatGram(weightGrams),
            caloriesLabel = NutritionFormatter.formatCalories(nutrition.calories),
            proteinsLabel = proteins,
            fatsLabel = fats,
            carbsLabel = carbs,
            macrosLabel = "Б $proteins • Ж $fats • У $carbs",
            costLabel = cost?.let { MoneyFormatter.format(it) },
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
    }
}
