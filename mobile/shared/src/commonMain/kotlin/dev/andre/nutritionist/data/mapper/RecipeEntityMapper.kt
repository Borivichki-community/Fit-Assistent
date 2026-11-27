package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.LocalRecipeEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeIngredientEntity
import dev.andre.nutritionist.data.local.entity.LocalRecipeWithIngredients
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeAuthorType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object RecipeEntityMapper {

    fun LocalRecipeWithIngredients.toDomain(): Recipe = Recipe(
        id = RecipeId(recipe.id),
        title = recipe.title,
        authorType = recipe.authorType.toAuthorType(),
        authorId = recipe.authorId?.takeIf { it.isNotBlank() }?.let(::UserId),
        originRecipeId = recipe.originRecipeId?.takeIf { it.isNotBlank() }?.let(::RecipeId),
        description = recipe.description,
        instructions = recipe.instructions,
        visibility = recipe.visibility.toVisibility(),
        servingsCount = recipe.servingsCount,
        totalWeightGrams = recipe.totalWeightGrams,
        nutrition = NutritionFacts(
            calories = recipe.caloriesPer100g,
            proteins = recipe.proteinsPer100g,
            fats = recipe.fatsPer100g,
            carbs = recipe.carbsPer100g,
        ),
        ingredients = ingredients.map { it.toDomain() },
        updatedAt = Instant.fromEpochMilliseconds(recipe.updatedAtEpochMs),
    )

    fun LocalRecipeIngredientEntity.toDomain(): RecipeIngredient = RecipeIngredient(
        id = RecipeIngredientId(id),
        productId = productId?.takeIf { it.isNotBlank() }?.let(::ProductId),
        name = productName,
        weightGrams = weightGrams,
        nutritionPer100g = NutritionFacts(
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        ),
    )

    fun Recipe.toEntity(
        syncStatus: SyncStatus = SyncStatus.PENDING,
        idempotencyKey: String = IdempotencyKey.generate().raw,
        isDeleted: Boolean = false,
        now: Instant = Clock.System.now(),
    ): LocalRecipeEntity = LocalRecipeEntity(
        id = id.raw,
        authorType = authorType.toDbName(),
        authorId = authorId?.raw,
        originRecipeId = originRecipeId?.raw,
        title = title,
        description = description,
        instructions = instructions,
        visibility = visibility.toDbName(),
        servingsCount = servingsCount,
        totalWeightGrams = totalWeightGrams,
        caloriesPer100g = nutrition.calories,
        proteinsPer100g = nutrition.proteins,
        fatsPer100g = nutrition.fats,
        carbsPer100g = nutrition.carbs,
        syncStatus = syncStatus.toDbName(),
        idempotencyKey = idempotencyKey,
        isDeleted = isDeleted,
        updatedAtEpochMs = (updatedAt ?: now).toEpochMilliseconds(),
    )

    @OptIn(ExperimentalUuidApi::class)
    fun RecipeIngredient.toEntity(recipeId: String): LocalRecipeIngredientEntity =
        LocalRecipeIngredientEntity(
            id = id.raw.ifBlank { Uuid.random().toString() },
            recipeId = recipeId,
            productId = productId?.raw,
            productName = name,
            weightGrams = weightGrams,
            calories = nutritionPer100g.calories,
            proteins = nutritionPer100g.proteins,
            fats = nutritionPer100g.fats,
            carbs = nutritionPer100g.carbs,
        )

    private fun RecipeAuthorType.toDbName(): String = when (this) {
        RecipeAuthorType.UNSPECIFIED -> RecipeAuthorType.UNSPECIFIED.name
        RecipeAuthorType.SYSTEM -> RecipeAuthorType.SYSTEM.name
        RecipeAuthorType.USER -> RecipeAuthorType.USER.name
        RecipeAuthorType.COACH -> RecipeAuthorType.COACH.name
        RecipeAuthorType.AI -> RecipeAuthorType.AI.name
    }

    private fun ContentVisibility.toDbName(): String = when (this) {
        ContentVisibility.UNSPECIFIED -> ContentVisibility.UNSPECIFIED.name
        ContentVisibility.PRIVATE -> ContentVisibility.PRIVATE.name
        ContentVisibility.LINK_ONLY -> ContentVisibility.LINK_ONLY.name
        ContentVisibility.PUBLIC -> ContentVisibility.PUBLIC.name
    }

    private fun SyncStatus.toDbName(): String = when (this) {
        SyncStatus.PENDING -> SyncStatus.PENDING.name
        SyncStatus.SYNCED -> SyncStatus.SYNCED.name
        SyncStatus.FAILED -> SyncStatus.FAILED.name
        SyncStatus.DELETED_PENDING -> SyncStatus.DELETED_PENDING.name
    }

    private fun String.toAuthorType(): RecipeAuthorType =
        RecipeAuthorType.entries.firstOrNull { it.name == uppercase() }
            ?: RecipeAuthorType.UNSPECIFIED

    private fun String.toVisibility(): ContentVisibility =
        ContentVisibility.entries.firstOrNull { it.name == uppercase() }
            ?: ContentVisibility.UNSPECIFIED
}
