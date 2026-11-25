package dev.andre.nutritionist.presentation.mappers

import dev.andre.nutritionist.domain.model.DailyNutritionBalance
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.DailyNutritionUiModel
import kotlin.math.abs

object DailyNutritionUiMapper {

    fun DailyNutritionBalance.toUiModel(): DailyNutritionUiModel {
        val caloriesProgressRatio = if (targetCalories > 0) {
            consumedCalories.toDouble() / targetCalories.toDouble()
        } else {
            0.0
        }
        val proteinsProgressRatio = if (proteins.targetGrams > 0.0) {
            proteins.consumedGrams / proteins.targetGrams
        } else {
            0.0
        }
        val fatsProgressRatio = if (fats.targetGrams > 0.0) {
            fats.consumedGrams / fats.targetGrams
        } else {
            0.0
        }
        val carbsProgressRatio = if (carbs.targetGrams > 0.0) {
            carbs.consumedGrams / carbs.targetGrams
        } else {
            0.0
        }

        val remainingLabel = if (isCaloriesExceeded) {
            "Перебор ${NutritionFormatter.formatCalories(abs(remainingCalories).toDouble())}"
        } else {
            "Осталось ${NutritionFormatter.formatCalories(remainingCalories.toDouble())}"
        }

        return DailyNutritionUiModel(
            targetCaloriesLabel = NutritionFormatter.formatCalories(targetCalories.toDouble()),
            consumedCaloriesLabel = NutritionFormatter.formatCalories(consumedCalories.toDouble()),
            remainingCaloriesLabel = remainingLabel,
            caloriesProgress = caloriesProgressRatio.toFloat().coerceIn(0f, 1f),
            caloriesProgressLabel = NutritionFormatter.formatPercentage(caloriesProgressRatio),
            isCaloriesExceeded = isCaloriesExceeded,
            proteinsConsumedLabel = NutritionFormatter.formatGram(proteins.consumedGrams),
            proteinsTargetLabel = NutritionFormatter.formatGram(proteins.targetGrams),
            proteinsProgress = proteinsProgressRatio.toFloat().coerceIn(0f, 1f),
            fatsConsumedLabel = NutritionFormatter.formatGram(fats.consumedGrams),
            fatsTargetLabel = NutritionFormatter.formatGram(fats.targetGrams),
            fatsProgress = fatsProgressRatio.toFloat().coerceIn(0f, 1f),
            carbsConsumedLabel = NutritionFormatter.formatGram(carbs.consumedGrams),
            carbsTargetLabel = NutritionFormatter.formatGram(carbs.targetGrams),
            carbsProgress = carbsProgressRatio.toFloat().coerceIn(0f, 1f),
        )
    }
}
