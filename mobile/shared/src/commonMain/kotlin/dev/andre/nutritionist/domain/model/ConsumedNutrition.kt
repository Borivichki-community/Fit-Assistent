package dev.andre.nutritionist.domain.model

/**
 * Иммутабельная модель фактического потребления нутриентов (КБЖУ) за день.
 */
data class ConsumedNutrition(
    val calories: Int = 0,
    val proteinsGrams: Double = 0.0,
    val fatsGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
)
