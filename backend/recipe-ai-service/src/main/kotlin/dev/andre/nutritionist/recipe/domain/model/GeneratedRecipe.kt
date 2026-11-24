package dev.andre.nutritionist.recipe.domain.model

import com.budgetntr.proto.recipe.v1.MealType
import com.budgetntr.proto.recipe.v1.MenuSource

data class TargetNutrition(
    val calories: Double,
    val proteinsGrams: Double,
    val fatsGrams: Double,
    val carbsGrams: Double,
)

data class MenuIngredient(
    val productId: String,
    val name: String,
    val grams: Int,
)

data class GeneratedMeal(
    val type: MealType,
    val title: String,
    val ingredients: List<MenuIngredient>,
    val nutrition: TargetNutrition,
    val estimatedCostMinor: Long,
    val cookingTimeMinutes: Int,
)

data class GeneratedMealPlan(
    val planId: String,
    val period: TargetPeriod,
    val meals: List<GeneratedMeal>,
    val totalNutrition: TargetNutrition,
    val estimatedCostMinor: Long,
    val currency: String,
    val cookingInstructions: String,
    val source: MenuSource,
)
