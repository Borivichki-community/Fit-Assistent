package dev.andre.nutritionist.domain.model

/**
 * Пищевая ценность (КБЖУ) на 100 г продукта или на всё блюдо.
 */
data class NutritionFacts(
    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0,
) {
    operator fun plus(other: NutritionFacts): NutritionFacts = NutritionFacts(
        calories = calories + other.calories,
        proteins = proteins + other.proteins,
        fats = fats + other.fats,
        carbs = carbs + other.carbs,
    )

    operator fun times(factor: Double): NutritionFacts = NutritionFacts(
        calories = calories * factor,
        proteins = proteins * factor,
        fats = fats * factor,
        carbs = carbs * factor,
    )

    companion object {
        val ZERO: NutritionFacts = NutritionFacts(0.0, 0.0, 0.0, 0.0)
    }
}
