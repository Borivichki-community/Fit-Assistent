package dev.andre.nutritionist.presentation.mappers

import dev.andre.nutritionist.domain.model.DailyNutritionBalance
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter
import dev.andre.nutritionist.presentation.model.DailyNutritionUiModel
import kotlin.math.abs
import kotlin.math.roundToLong

object DailyNutritionUiMapper {

    fun DailyNutritionBalance.toUiModel(
        totalCost: Money = Money.ZERO,
        weeklyBudgetRubles: Double = 0.0,
    ): DailyNutritionUiModel {
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

        val sanitizedWeeklyBudget = if (weeklyBudgetRubles.isNaN() || weeklyBudgetRubles.isInfinite() || weeklyBudgetRubles < 0.0) {
            0.0
        } else {
            weeklyBudgetRubles
        }
        val dailyBudgetRubles = (sanitizedWeeklyBudget / 7.0).roundToLong()
        val dailyBudgetFormatted = MoneyFormatter.format(Money.fromMinor(dailyBudgetRubles * 100L))

        val proteinsTargetVal = NutritionFormatter.formatMacroValue(proteins.targetGrams)
        val fatsTargetVal = NutritionFormatter.formatMacroValue(fats.targetGrams)
        val carbsTargetVal = NutritionFormatter.formatMacroValue(carbs.targetGrams)

        return DailyNutritionUiModel(
            targetCaloriesLabel = NutritionFormatter.formatCalories(targetCalories.toDouble()),
            consumedCaloriesLabel = NutritionFormatter.formatCalories(consumedCalories.toDouble()),
            remainingCaloriesLabel = remainingLabel,
            caloriesConsumedValue = NutritionFormatter.formatCaloriesNumber(consumedCalories.toDouble()),
            caloriesTargetValue = "/ ${NutritionFormatter.formatCaloriesNumber(targetCalories.toDouble())}",
            caloriesProgress = caloriesProgressRatio.toFloat().coerceIn(0f, 1f),
            caloriesProgressLabel = NutritionFormatter.formatPercentage(caloriesProgressRatio),
            isCaloriesExceeded = isCaloriesExceeded,
            proteinsConsumedLabel = NutritionFormatter.formatGram(proteins.consumedGrams),
            proteinsTargetLabel = "$proteinsTargetVal г",
            proteinsConsumedValue = NutritionFormatter.formatMacroValue(proteins.consumedGrams),
            proteinsTargetShortLabel = "/ $proteinsTargetVal г",
            proteinsProgress = proteinsProgressRatio.toFloat().coerceIn(0f, 1f),
            fatsConsumedLabel = NutritionFormatter.formatGram(fats.consumedGrams),
            fatsTargetLabel = "$fatsTargetVal г",
            fatsConsumedValue = NutritionFormatter.formatMacroValue(fats.consumedGrams),
            fatsTargetShortLabel = "/ $fatsTargetVal г",
            fatsProgress = fatsProgressRatio.toFloat().coerceIn(0f, 1f),
            carbsConsumedLabel = NutritionFormatter.formatGram(carbs.consumedGrams),
            carbsTargetLabel = "$carbsTargetVal г",
            carbsConsumedValue = NutritionFormatter.formatMacroValue(carbs.consumedGrams),
            carbsTargetShortLabel = "/ $carbsTargetVal г",
            carbsProgress = carbsProgressRatio.toFloat().coerceIn(0f, 1f),
            totalMealCostLabel = MoneyFormatter.format(totalCost),
            dailyBudgetLabel = "Бюджет: ~$dailyBudgetFormatted",
        )
    }
}
