package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.SavedRecipeEntity
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import kotlin.time.Clock
import kotlin.time.Instant

object SavedRecipeEntityMapper {

    fun SavedRecipeEntity.toDomain(): Recipe = Recipe(
        id = RecipeId(id),
        title = title,
        mealType = mealType.toMealType(),
        nutrition = NutritionFacts(
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        ),
        estimatedCost = Money.fromMinor(costMinor),
        ingredients = decodeIngredientsJson(ingredientsJson),
        instructions = instructions.takeIf { it.isNotEmpty() },
        updatedAt = Instant.fromEpochMilliseconds(savedAt),
    )

    fun Recipe.toSavedEntity(
        planId: String? = null,
        savedAt: Instant = Clock.System.now(),
    ): SavedRecipeEntity = SavedRecipeEntity(
        id = id.raw,
        planId = planId,
        title = title,
        mealType = mealType.toDbName(),
        calories = nutrition.calories,
        proteins = nutrition.proteins,
        fats = nutrition.fats,
        carbs = nutrition.carbs,
        costMinor = estimatedCost.amountMinor,
        ingredientsJson = encodeIngredientsJson(ingredients),
        instructions = instructions.orEmpty(),
        savedAt = savedAt.toEpochMilliseconds(),
    )

    internal fun encodeIngredientsJson(ingredients: List<RecipeIngredient>): String =
        buildString {
            append('[')
            ingredients.forEachIndexed { index, ing ->
                if (index > 0) append(',')
                append('{')
                append("\"id\":")
                appendJsonString(ing.id.raw)
                append(",\"productId\":")
                if (ing.productId != null) appendJsonString(ing.productId.raw) else append("null")
                append(",\"name\":")
                appendJsonString(ing.name)
                append(",\"weightGrams\":")
                append(ing.weightGrams)
                append(",\"calories\":")
                append(ing.nutritionPer100g.calories)
                append(",\"proteins\":")
                append(ing.nutritionPer100g.proteins)
                append(",\"fats\":")
                append(ing.nutritionPer100g.fats)
                append(",\"carbs\":")
                append(ing.nutritionPer100g.carbs)
                append(",\"costMinor\":")
                append(ing.cost?.amountMinor?.toString() ?: "null")
                append('}')
            }
            append(']')
        }

    internal fun decodeIngredientsJson(json: String): List<RecipeIngredient> {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        val parsed = PureJsonParser(trimmed).parseArrayOrNull() ?: return emptyList()
        return parsed.mapNotNull { obj ->
            val name = obj["name"] as? String ?: return@mapNotNull null
            val id = (obj["id"] as? String).orEmpty()
            val productId = (obj["productId"] as? String)?.takeIf { it.isNotEmpty() }?.let(::ProductId)
            val weightGrams = (obj["weightGrams"] as? Number)?.toDouble() ?: 0.0
            val calories = (obj["calories"] as? Number)?.toDouble() ?: 0.0
            val proteins = (obj["proteins"] as? Number)?.toDouble() ?: 0.0
            val fats = (obj["fats"] as? Number)?.toDouble() ?: 0.0
            val carbs = (obj["carbs"] as? Number)?.toDouble() ?: 0.0
            val costMinor = (obj["costMinor"] as? Number)?.toLong()
            RecipeIngredient(
                id = RecipeIngredientId(id),
                productId = productId,
                name = name,
                weightGrams = weightGrams,
                nutritionPer100g = NutritionFacts(
                    calories = calories,
                    proteins = proteins,
                    fats = fats,
                    carbs = carbs,
                ),
                cost = costMinor?.let { Money.fromMinor(it) },
            )
        }
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }

    private fun MealType.toDbName(): String = when (this) {
        MealType.BREAKFAST -> MealType.BREAKFAST.name
        MealType.LUNCH -> MealType.LUNCH.name
        MealType.DINNER -> MealType.DINNER.name
        MealType.SNACK -> MealType.SNACK.name
    }

    private fun String.toMealType(): MealType =
        MealType.entries.firstOrNull { it.name == uppercase() } ?: MealType.LUNCH

    private class PureJsonParser(private val input: String) {
        // mutable by design: cursor index in recursive-descent parser
        private var pos = 0

        fun parseArrayOrNull(): List<Map<String, Any?>>? = try {
            skipWhitespace()
            if (peek() != '[') return null
            pos++
            val result = mutableListOf<Map<String, Any?>>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return result
            }
            while (pos < input.length) {
                skipWhitespace()
                result.add(parseObject())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return result
                    }
                    else -> return null
                }
            }
            null
        } catch (_: Exception) {
            null
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (pos < input.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Unexpected char in object at $pos")
                }
            }
            throw IllegalArgumentException("Unterminated object")
        }

        private fun parseValue(): Any? = when (val ch = peek()) {
            '"' -> parseString()
            'n' -> parseNull()
            't', 'f' -> parseBoolean()
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected value start '$ch' at $pos")
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val ch = input[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= input.length) throw IllegalArgumentException("Unterminated escape")
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = input.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseNull(): Any? {
            if (input.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("Invalid null at $pos")
        }

        private fun parseBoolean(): Boolean = when {
            input.startsWith("true", pos) -> {
                pos += 4
                true
            }
            input.startsWith("false", pos) -> {
                pos += 5
                false
            }
            else -> throw IllegalArgumentException("Invalid boolean at $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            while (pos < input.length && input[pos] in "-0123456789.eE+") {
                pos++
            }
            val token = input.substring(start, pos)
            return if (token.contains('.') || token.contains('e') || token.contains('E')) {
                token.toDouble()
            } else {
                token.toLong()
            }
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char =
            if (pos < input.length) input[pos] else '\u0000'

        private fun expect(expected: Char) {
            if (pos >= input.length || input[pos] != expected) {
                throw IllegalArgumentException("Expected '$expected' at $pos")
            }
            pos++
        }
    }
}
