package dev.andre.nutritionist.domain.model

import kotlin.jvm.JvmInline

/**
 * Идентификатор плана питания.
 */
@JvmInline
value class MealPlanId(val raw: String)

/**
 * Доменная модель готового или сгенерированного плана питания (рациона).
 */
data class MealPlan(
    val id: MealPlanId,
    val title: String,
    val description: String,
    val category: String,
    val targetCalories: Int,
    val proteinsGrams: Double,
    val fatsGrams: Double,
    val carbsGrams: Double,
    val estimatedWeeklyBudgetRub: Double,
    val difficulty: String = "Легко",
    val durationDays: Int = 7,
    val rating: Double = 4.8,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
)
