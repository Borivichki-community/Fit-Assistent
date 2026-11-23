package dev.andre.nutritionist.domain.model

import kotlin.jvm.JvmInline

/**
 * Идентификатор плана питания.
 */
@JvmInline
value class MealPlanId(val raw: String)

/**
 * Идентификатор позиции списка покупок.
 */
@JvmInline
value class GroceryItemId(val raw: String)

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

/**
 * Отдельный прием пищи внутри дня рациона.
 */
data class MealPlanMealItem(
    val title: String,
    val mealType: String,
    val calories: Int,
    val proteinsGrams: Double,
    val fatsGrams: Double,
    val carbsGrams: Double,
)

/**
 * Расписание приемов пищи на конкретный день плана питания.
 */
data class MealPlanDay(
    val dayNumber: Int,
    val dayName: String,
    val shortDayName: String,
    val targetCalories: Int,
    val meals: List<MealPlanMealItem>,
)

/**
 * Позиция продуктовой корзины, сгенерированная на основе активного рациона.
 */
data class GroceryItem(
    val id: GroceryItemId,
    val name: String,
    val amount: String,
    val category: String,
    val isBought: Boolean = false,
    val estimatedPriceRub: Double = 0.0,
)

