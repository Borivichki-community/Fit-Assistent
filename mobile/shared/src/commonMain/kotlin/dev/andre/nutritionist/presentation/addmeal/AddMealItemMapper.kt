package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.calculator.NutritionCalculator
import dev.andre.nutritionist.domain.calculator.RecipeNutritionCalculator
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter

/**
 * Чистый маппер доменных продуктов и рецептов в [AddMealSearchItemUiModel].
 */
object AddMealItemMapper {

    const val DEFAULT_PRODUCT_PORTION_GRAMS = 100.0

    fun singleServingWeightGrams(recipe: Recipe): Double {
        val totalWeight = if (recipe.totalWeightGrams > 0.0) {
            recipe.totalWeightGrams
        } else {
            RecipeNutritionCalculator.calculateSummary(recipe.ingredients).totalWeightGrams
        }
        return if (totalWeight > 0.0) {
            totalWeight / recipe.servingsCount.coerceAtLeast(1).toDouble()
        } else {
            DEFAULT_PRODUCT_PORTION_GRAMS
        }
    }

    fun Product.toSearchItemUiModel(): AddMealSearchItemUiModel {
        val portionNutrition = NutritionCalculator.calculatePortion(
            nutritionPer100g = nutritionPer100g,
            weightGrams = DEFAULT_PRODUCT_PORTION_GRAMS,
        )
        val price = prices.firstOrNull()?.price?.let {
            NutritionCalculator.calculatePortionCost(it, DEFAULT_PRODUCT_PORTION_GRAMS)
        }
        return AddMealSearchItemUiModel(
            id = id.raw,
            kind = AddMealItemKind.PRODUCT,
            title = name,
            typeLabel = "Продукт",
            portionLabel = NutritionFormatter.formatGram(DEFAULT_PRODUCT_PORTION_GRAMS),
            caloriesLabel = NutritionFormatter.formatCalories(portionNutrition.calories),
            macrosLabel = formatCompactMacros(portionNutrition),
            priceLabel = price?.let { MoneyFormatter.format(it) },
        )
    }

    fun Recipe.toSearchItemUiModel(): AddMealSearchItemUiModel {
        val weightGrams = singleServingWeightGrams(this)
        val per100g = if (nutrition != NutritionFacts.ZERO) {
            nutrition
        } else {
            RecipeNutritionCalculator.calculatePer100g(ingredients)
        }
        val portionNutrition = RecipeNutritionCalculator.calculatePortion(
            nutritionPer100g = per100g,
            weightGrams = weightGrams,
        )
        val totalRecipeWeight = if (totalWeightGrams > 0.0) {
            totalWeightGrams
        } else {
            RecipeNutritionCalculator.calculateSummary(ingredients).totalWeightGrams
        }
        val portionCost = RecipeNutritionCalculator.calculatePortionCost(
            totalCost = estimatedCost,
            totalWeightGrams = totalRecipeWeight,
            portionWeightGrams = weightGrams,
        )
        return AddMealSearchItemUiModel(
            id = id.raw,
            kind = AddMealItemKind.RECIPE,
            title = title,
            typeLabel = "Рецепт",
            portionLabel = NutritionFormatter.formatGram(weightGrams),
            caloriesLabel = NutritionFormatter.formatCalories(portionNutrition.calories),
            macrosLabel = formatCompactMacros(portionNutrition),
            priceLabel = portionCost?.let { MoneyFormatter.format(it) },
        )
    }

    private fun formatCompactMacros(nutrition: NutritionFacts): String {
        val p = NutritionFormatter.formatMacroValue(nutrition.proteins)
        val f = NutritionFormatter.formatMacroValue(nutrition.fats)
        val c = NutritionFormatter.formatMacroValue(nutrition.carbs)
        return "Б: ${p}г • Ж: ${f}г • У: ${c}г"
    }
}
