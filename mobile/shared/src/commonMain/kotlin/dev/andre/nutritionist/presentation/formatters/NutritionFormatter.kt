package dev.andre.nutritionist.presentation.formatters

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Чистый KMP-форматтер калорий, макронутриентов и прогресса.
 */
object NutritionFormatter {

    fun formatCalories(calories: Double): String {
        val sanitized = if (calories.isNaN() || calories.isInfinite()) 0.0 else calories
        val rounded = sanitized.roundToLong()
        val isNegative = rounded < 0L
        val absValue = if (rounded == Long.MIN_VALUE) Long.MAX_VALUE else abs(rounded)
        val prefix = if (isNegative) "-" else ""
        return "$prefix${MoneyFormatter.formatWithThousandsSeparator(absValue)} ккал"
    }

    fun formatGram(grams: Double): String {
        val sanitized = if (grams.isNaN() || grams.isInfinite()) 0.0 else grams
        val tenths = (sanitized * 10.0).roundToLong()
        val isNegative = tenths < 0L
        val absTenths = if (tenths == Long.MIN_VALUE) Long.MAX_VALUE else abs(tenths)
        val whole = absTenths / 10L
        val decimal = absTenths % 10L
        val prefix = if (isNegative) "-" else ""
        val wholeStr = MoneyFormatter.formatWithThousandsSeparator(whole)
        return if (decimal == 0L) {
            "$prefix$wholeStr г"
        } else {
            "$prefix$wholeStr,$decimal г"
        }
    }

    /**
     * Форматирует прогресс (0.0..1.0+) в процентную строку, например `0.74 -> "74%"`.
     */
    fun formatPercentage(progress: Double): String {
        val sanitized = if (progress.isNaN() || progress.isInfinite()) 0.0 else progress.coerceAtLeast(0.0)
        val percent = (sanitized * 100.0).roundToInt()
        return "$percent%"
    }
}
