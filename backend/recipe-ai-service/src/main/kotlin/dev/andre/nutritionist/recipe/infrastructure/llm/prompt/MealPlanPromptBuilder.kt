package dev.andre.nutritionist.recipe.infrastructure.llm.prompt

import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.MealPlanRequest
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.TargetPeriod
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class MealPlanPromptBuilder(
    private val json: JsonMapper,
) {
    fun buildMealPlanPrompt(
        request: MealPlanRequest,
        products: List<CatalogProduct>,
    ): String {
        val candidates =
            products.map { p ->
                mapOf(
                    "id" to p.id.raw,
                    "name" to p.name,
                    "kcalPer100g" to p.caloriesPer100g,
                    "proteinsPer100g" to p.proteinsPer100g,
                    "fatsPer100g" to p.fatsPer100g,
                    "carbsPer100g" to p.carbsPer100g,
                    "priceMinorPer100g" to p.pricePer100gMinor,
                )
            }
        val targetDesc =
            when (request.period) {
                TargetPeriod.DAY, TargetPeriod.UNSPECIFIED -> "рацион на полный день (завтрак, обед, ужин и перекус)"
                TargetPeriod.BREAKFAST -> "одно блюдо на завтрак (type: breakfast)"
                TargetPeriod.LUNCH -> "одно блюдо на обед (type: lunch)"
                TargetPeriod.DINNER -> "одно блюдо на ужин (type: dinner)"
                TargetPeriod.SNACK -> "один перекус (type: snack)"
            }
        val prefs =
            if (request.preferredProducts.isNotEmpty()) {
                "Предпочтения пользователя: ${request.preferredProducts.joinToString(", ")}."
            } else {
                ""
            }
        val excludes =
            if (request.excludedProducts.isNotEmpty()) {
                "Исключить аллергены/продукты: ${request.excludedProducts.joinToString(", ")}."
            } else {
                ""
            }
        val note =
            if (request.userNote.isNotBlank()) {
                "Дополнительные пожелания: ${request.userNote}."
            } else {
                ""
            }

        return """
            Составь $targetDesc.
            Целевые показатели: калории: ${request.targetNutrition.calories} ккал, белки: ${request.targetNutrition.proteinsGrams} г, жиры: ${request.targetNutrition.fatsGrams} г, углеводы: ${request.targetNutrition.carbsGrams} г.
            Максимальный бюджет: ${request.maxBudgetMinor} копеек (${request.maxBudgetMinor / 100} руб).
            $prefs
            $excludes
            $note
            Для каждого блюда укажи type (breakfast/lunch/dinner/snack), title на русском, время приготовления в минутах cooking_time_minutes и список используемых продуктов (id и целые граммы от 1 до 2000).
            Дай подробные пошаговые инструкции приготовления cooking_instructions на русском языке.
            Используй ТОЛЬКО продукты из предоставленного каталога, рассчитывая вес пропорционально данным на 100г.
            Верни JSON: {"cooking_instructions":"...","meals":[{"type":"...","title":"...","cooking_time_minutes":15,"products":[{"id":"...","grams":100}]}]}
            Каталог продуктов: ${json.writeValueAsString(candidates)}
            """.trimIndent()
    }

    fun buildWeekMenuPrompt(
        request: MenuRequest,
        products: List<CatalogProduct>,
    ): String {
        val candidates =
            products.map { product ->
                mapOf(
                    "id" to product.id.raw,
                    "name" to product.name,
                    "kcalPer100g" to product.caloriesPer100g,
                    "priceMinorPer100g" to product.pricePer100gMinor,
                    "preferred" to (product.id in request.preferred),
                )
            }
        return """
            Составь меню из 7 дней (day от 1 до 7). На каждый день нужны breakfast, lunch и dinner.
            Дай каждому приёму пищи краткое название на русском. Для каждого из 1–8 продуктов укажи id и целые граммы (1–2000).
            Используй только продукты ниже, без повторов продукта в одном приёме пищи. Отдавай предпочтение preferred.
            Цель на каждый день: ${request.dailyCalories} ккал. Стоимость за 7 дней не выше ${request.weeklyBudgetMinor} копеек.
            Считай пропорционально граммам по данным на 100 г. Верни JSON по заданной схеме.
            Каталог продуктов: ${json.writeValueAsString(candidates)}
            """.trimIndent()
    }
}
