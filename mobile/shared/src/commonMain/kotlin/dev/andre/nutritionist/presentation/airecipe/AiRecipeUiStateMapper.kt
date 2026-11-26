package dev.andre.nutritionist.presentation.airecipe

import dev.andre.nutritionist.domain.model.*
import dev.andre.nutritionist.presentation.formatters.MoneyFormatter
import dev.andre.nutritionist.presentation.formatters.NutritionFormatter as N

data class AiRecipeUiState(val slot: String, val content: Content) {
    sealed interface Content {
        data class Configuring(
            val prompt: AiRecipePrompt,
            val budgetLabel: String,
            val budgets: Map<Int, String>,
            val nutrition: List<Pair<String, String>>,
        ) : Content
        data class Generating(val phase: String, val progress: Float, val percent: String) : Content
        data class Success(val title: String, val description: String, val badges: String, val macros: List<Pair<String,Float>>, val ingredients: List<String>, val instructions: String, val addLabel: String, val enabled: Boolean) : Content
        data class Error(val message: String) : Content
    }
}

class AiRecipeUiStateMapper {
    private val slots = mapOf(MealType.BREAKFAST to "Завтрак", MealType.LUNCH to "Обед", MealType.DINNER to "Ужин", MealType.SNACK to "Перекус")
    fun map(state: AiRecipeState): AiRecipeUiState {
        val slot = slots.getValue(state.prompt.mealType)
        val content = when (val current = state.content) {
            AiRecipeState.Content.Configuring -> {
                val prompt = state.prompt
                val nutrition = prompt.nutritionTarget
                fun budgetLabel(amount: Int) = MoneyFormatter.format(Money.fromRubles(amount.toDouble()))
                val budgets = linkedMapOf(
                    200 to "до " + budgetLabel(200),
                    350 to budgetLabel(350),
                    500 to budgetLabel(500),
                    AiRecipePrompt.NO_BUDGET_LIMIT to "Без лимита",
                )
                AiRecipeUiState.Content.Configuring(
                    prompt = prompt,
                    budgetLabel = if (prompt.targetBudgetRub == AiRecipePrompt.NO_BUDGET_LIMIT) {
                        budgets.getValue(AiRecipePrompt.NO_BUDGET_LIMIT)
                    } else {
                        budgetLabel(prompt.targetBudgetRub)
                    },
                    budgets = budgets,
                    nutrition = listOf(
                        "Ккал" to N.formatCaloriesNumber(nutrition.calories),
                        "Белки" to N.formatGram(nutrition.proteins).takeIf { nutrition.proteins > 0 }.orEmpty().ifEmpty { "—" },
                        "Жиры" to N.formatGram(nutrition.fats).takeIf { nutrition.fats > 0 }.orEmpty().ifEmpty { "—" },
                        "Углеводы" to N.formatGram(nutrition.carbs).takeIf { nutrition.carbs > 0 }.orEmpty().ifEmpty { "—" },
                    ),
                )
            }
            is AiRecipeState.Content.Generating -> AiRecipeUiState.Content.Generating(current.phaseText,current.progress,N.formatPercentage(current.progress.toDouble()))
            is AiRecipeState.Content.Error -> AiRecipeUiState.Content.Error(current.message)
            is AiRecipeState.Content.Success -> {
                val recipe = current.recipe
                val servings = recipe.servingsCount.coerceAtLeast(1)
                val weight = recipe.totalWeightGrams.takeIf { it > 0 } ?: recipe.ingredients.sumOf { it.weightGrams }
                val nutrition = recipe.nutrition * (weight / servings / 100)
                val energy = nutrition.proteins * 4 + nutrition.fats * 9 + nutrition.carbs * 4
                fun macro(label: String, grams: Double, factor: Int) = (label + " · " + N.formatGram(grams)) to
                    (if (energy > 0) grams * factor / energy else 0.0).toFloat()
                AiRecipeUiState.Content.Success(recipe.title,recipe.description.orEmpty(),
                    listOfNotNull(N.formatCalories(nutrition.calories),MoneyFormatter.format(recipe.estimatedCost / servings.toLong()),recipe.cookingTimeMinutes.takeIf { it > 0 }?.let { "$it мин" }).joinToString(" • "),
                    listOf(macro("Белки",nutrition.proteins,4),macro("Жиры",nutrition.fats,9),macro("Углеводы",nutrition.carbs,4)),
                    recipe.ingredients.map { listOfNotNull(it.name,N.formatGram(it.weightGrams),it.cost?.let(MoneyFormatter::format) ?: "Цена недоступна").joinToString(" · ") },
                    recipe.instructions.orEmpty(),"Добавить в " + slot.lowercase(),!current.isSaving)
            }
        }
        return AiRecipeUiState(slot,content)
    }
}

