package dev.andre.nutritionist.diary.domain.calculator

import dev.andre.nutritionist.diary.domain.model.DailyEntries
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.MealNutritionSummary
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import kotlin.math.roundToLong

object DiaryNutritionAggregator {
    fun aggregate(entries: List<DiaryEntry>): DailyEntries {
        val grouped = entries.groupBy { it.mealType }
        val meals =
            MealType.entries.map { mealType ->
                MealNutritionSummary(mealType, sum(grouped[mealType].orEmpty().map { it.nutrition }))
            }
        return DailyEntries(entries, sum(meals.map { it.nutrition }), meals)
    }

    private fun sum(values: List<NutritionSummary>): NutritionSummary =
        NutritionSummary(
            calories =
                values.fold(0) { total, value ->
                    if (value.calories < 0 || total > Int.MAX_VALUE - value.calories) {
                        throw ArithmeticException("Calories total exceeds supported range")
                    }
                    total + value.calories
                },
            proteins = sumGrams(values.map { it.proteins }),
            fats = sumGrams(values.map { it.fats }),
            carbs = sumGrams(values.map { it.carbs }),
        )

    private fun sumGrams(values: List<Double>): Double =
        values.fold(0L) { total, value ->
            if (!value.isFinite() || value < 0.0 || value * 100 >= Long.MAX_VALUE.toDouble()) {
                throw ArithmeticException("Nutrition total exceeds supported range")
            }
            val hundredths = (value * 100).roundToLong()
            if (total > Long.MAX_VALUE - hundredths) {
                throw ArithmeticException("Nutrition total exceeds supported range")
            }
            total + hundredths
        } / 100.0
}
