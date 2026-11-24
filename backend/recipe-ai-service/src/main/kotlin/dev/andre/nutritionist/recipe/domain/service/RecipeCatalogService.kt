package dev.andre.nutritionist.recipe.domain.service

import com.budgetntr.proto.recipe.v1.RecipeIngredientProto
import dev.andre.nutritionist.recipe.infrastructure.redis.RedisMenuStore
import dev.andre.nutritionist.recipe.infrastructure.security.AuthContext
import dev.andre.nutritionist.recipe.persistence.entity.RecipeEntity
import dev.andre.nutritionist.recipe.persistence.entity.RecipeIngredientEntity
import dev.andre.nutritionist.recipe.persistence.repository.KeysetSearchResult
import dev.andre.nutritionist.recipe.persistence.repository.RecipeRepository
import dev.andre.nutritionist.recipe.persistence.repository.RecipeSearchCriteria
import dev.andre.nutritionist.recipe.persistence.repository.RecipeSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class RecipeCatalogService(
    private val recipeRepository: RecipeRepository,
    private val recipeSearchRepository: RecipeSearchRepository,
    private val coachingAccessService: CoachingAccessService,
    private val redisStore: RedisMenuStore,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun searchRecipes(
        query: String?,
        authorType: String?,
        visibility: String?,
        pageSize: Int,
        pageToken: String?,
        authContext: AuthContext,
    ): KeysetSearchResult<RecipeEntity> {
        val currentUserId = authContext.userId
        val allowedRecipeIds =
            currentUserId?.let {
                coachingAccessService.getAllowedTargetIds(it, "RECIPE")
            } ?: emptySet()

        val parsedAuthorType =
            authorType
                ?.takeIf { it.isNotBlank() && it != "AUTHOR_TYPE_UNSPECIFIED" }
                ?.removePrefix("AUTHOR_TYPE_")

        val parsedVisibility =
            visibility
                ?.takeIf { it.isNotBlank() && it != "CONTENT_VISIBILITY_UNSPECIFIED" }
                ?.removePrefix("CONTENT_VISIBILITY_")

        val criteria =
            RecipeSearchCriteria(
                query = query,
                authorType = parsedAuthorType,
                visibility = parsedVisibility,
                currentUserId = currentUserId,
                allowedRecipeIds = allowedRecipeIds,
                pageSize = pageSize,
                pageToken = pageToken,
            )
        return recipeSearchRepository.searchRecipes(criteria)
    }

    @Transactional(readOnly = true)
    fun getRecipe(
        recipeId: UUID,
        authContext: AuthContext,
    ): RecipeEntity {
        val recipe =
            recipeRepository.findWithIngredientsById(recipeId)
                ?: throw NoSuchElementException("Recipe with id $recipeId not found")

        checkReadAccess(recipe, authContext)
        return recipe
    }

    @Transactional
    fun createRecipe(
        title: String,
        description: String?,
        instructions: String?,
        visibility: String,
        servingsCount: Int,
        ingredients: List<RecipeIngredientProto>,
        authContext: AuthContext,
    ): RecipeEntity {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to create recipe" }
        require(title.isNotBlank()) { "Recipe title must not be blank" }
        require(servingsCount > 0) { "servingsCount must be greater than zero" }

        val authorType = if (authContext.hasRole(AuthContext.ROLE_COACH)) "COACH" else "USER"
        val cleanVisibility = visibility.removePrefix("CONTENT_VISIBILITY_").ifBlank { "PRIVATE" }

        val recipe =
            RecipeEntity(
                id = UUID.randomUUID(),
                authorType = authorType,
                authorId = currentUserId,
                title = title,
                description = description,
                instructions = instructions,
                visibility = cleanVisibility,
                servingsCount = servingsCount,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        applyIngredientsAndCalculateNutrition(recipe, ingredients)
        val saved = recipeRepository.save(recipe)
        logger.info("Created recipe id={} title='{}' by user={}", saved.id, saved.title, currentUserId)
        return saved
    }

    @Transactional
    fun updateRecipe(
        recipeId: UUID,
        title: String,
        description: String?,
        instructions: String?,
        visibility: String,
        servingsCount: Int,
        ingredients: List<RecipeIngredientProto>,
        authContext: AuthContext,
    ): RecipeEntity {
        val recipe =
            recipeRepository.findWithIngredientsById(recipeId)
                ?: throw NoSuchElementException("Recipe with id $recipeId not found")

        checkWriteAccess(recipe, authContext)
        require(title.isNotBlank()) { "Recipe title must not be blank" }
        require(servingsCount > 0) { "servingsCount must be greater than zero" }

        recipe.title = title
        recipe.description = description
        recipe.instructions = instructions
        recipe.visibility = visibility.removePrefix("CONTENT_VISIBILITY_").ifBlank { recipe.visibility }
        recipe.servingsCount = servingsCount
        recipe.updatedAt = Instant.now()

        recipe.ingredients.clear()
        applyIngredientsAndCalculateNutrition(recipe, ingredients)

        val updated = recipeRepository.save(recipe)
        logger.info("Updated recipe id={} title='{}'", updated.id, updated.title)
        return updated
    }

    @Transactional
    fun deleteRecipe(
        recipeId: UUID,
        authContext: AuthContext,
    ): Boolean {
        val recipe =
            recipeRepository.findById(recipeId).orElseThrow {
                NoSuchElementException("Recipe with id $recipeId not found")
            }
        checkWriteAccess(recipe, authContext)

        recipeRepository.delete(recipe)
        logger.info("Deleted recipe id={}", recipeId)
        return true
    }

    @Transactional
    fun forkRecipe(
        originRecipeId: UUID,
        newTitle: String?,
        authContext: AuthContext,
    ): RecipeEntity {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to fork recipe" }
        val origin =
            recipeRepository.findWithIngredientsById(originRecipeId)
                ?: throw NoSuchElementException("Origin recipe with id $originRecipeId not found")

        checkReadAccess(origin, authContext)

        val forkTitle = if (!newTitle.isNullOrBlank()) newTitle else "${origin.title} (Копия)"
        val authorType = if (authContext.hasRole(AuthContext.ROLE_COACH)) "COACH" else "USER"

        val forked =
            RecipeEntity(
                id = UUID.randomUUID(),
                authorType = authorType,
                authorId = currentUserId,
                originRecipe = origin,
                title = forkTitle,
                description = origin.description,
                instructions = origin.instructions,
                visibility = "PRIVATE",
                servingsCount = origin.servingsCount,
                totalWeightG = origin.totalWeightG,
                caloriesPer100g = origin.caloriesPer100g,
                proteinsPer100g = origin.proteinsPer100g,
                fatsPer100g = origin.fatsPer100g,
                carbsPer100g = origin.carbsPer100g,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        origin.ingredients.forEach { item ->
            val forkedItem =
                RecipeIngredientEntity(
                    id = UUID.randomUUID(),
                    recipe = forked,
                    productId = item.productId,
                    productNameSnapshot = item.productNameSnapshot,
                    weightGrams = item.weightGrams,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinsPer100g = item.proteinsPer100g,
                    fatsPer100g = item.fatsPer100g,
                    carbsPer100g = item.carbsPer100g,
                )
            forked.ingredients.add(forkedItem)
        }

        val saved = recipeRepository.save(forked)
        logger.info("Forked recipe originId={} to newId={} for user={}", originRecipeId, saved.id, currentUserId)
        return saved
    }

    @Transactional
    fun publishRecipe(
        recipeId: UUID,
        visibility: String,
        authContext: AuthContext,
    ): RecipeEntity {
        val recipe =
            recipeRepository.findWithIngredientsById(recipeId)
                ?: throw NoSuchElementException("Recipe with id $recipeId not found")
        checkWriteAccess(recipe, authContext)

        val cleanVis = visibility.removePrefix("CONTENT_VISIBILITY_").ifBlank { "PUBLIC" }
        recipe.visibility = cleanVis
        recipe.updatedAt = Instant.now()
        val saved = recipeRepository.save(recipe)
        logger.info("Published recipe id={} with visibility={}", saved.id, cleanVis)
        return saved
    }

    private fun checkReadAccess(
        recipe: RecipeEntity,
        authContext: AuthContext,
    ) {
        if (recipe.visibility.equals("PUBLIC", ignoreCase = true)) return
        val currentUserId = authContext.userId
        if (currentUserId != null && recipe.authorId == currentUserId) return
        if (authContext.isAdmin) return
        if (currentUserId != null && coachingAccessService.hasActiveAccessGrant(currentUserId, "RECIPE", recipe.id)) return

        throw SecurityException("Access denied to recipe ${recipe.id}")
    }

    private fun checkWriteAccess(
        recipe: RecipeEntity,
        authContext: AuthContext,
    ) {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to modify recipe" }
        if (recipe.authorId == currentUserId || authContext.isAdmin) return
        throw SecurityException("Only recipe author or admin can modify this recipe")
    }

    private fun applyIngredientsAndCalculateNutrition(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientProto>,
    ) {
        var totalWeight = BigDecimal.ZERO
        var totalCalories = BigDecimal.ZERO
        var totalProteins = BigDecimal.ZERO
        var totalFats = BigDecimal.ZERO
        var totalCarbs = BigDecimal.ZERO

        ingredients.forEach { dto ->
            val weight = BigDecimal.valueOf(dto.weightGrams).coerceAtLeast(BigDecimal.ZERO)
            val cal100 = BigDecimal.valueOf(dto.caloriesPer100G).coerceAtLeast(BigDecimal.ZERO)
            val prot100 = BigDecimal.valueOf(dto.proteinsPer100G).coerceAtLeast(BigDecimal.ZERO)
            val fat100 = BigDecimal.valueOf(dto.fatsPer100G).coerceAtLeast(BigDecimal.ZERO)
            val carb100 = BigDecimal.valueOf(dto.carbsPer100G).coerceAtLeast(BigDecimal.ZERO)

            totalWeight = totalWeight.add(weight)

            val factor = weight.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
            totalCalories = totalCalories.add(cal100.multiply(factor))
            totalProteins = totalProteins.add(prot100.multiply(factor))
            totalFats = totalFats.add(fat100.multiply(factor))
            totalCarbs = totalCarbs.add(carb100.multiply(factor))

            val prodId = if (dto.productId.isNotBlank()) UUID.fromString(dto.productId) else null
            val item =
                RecipeIngredientEntity(
                    id = if (dto.id.isNotBlank()) UUID.fromString(dto.id) else UUID.randomUUID(),
                    recipe = recipe,
                    productId = prodId,
                    productNameSnapshot = dto.productNameSnapshot.ifBlank { "Ингредиент" },
                    weightGrams = weight.setScale(2, RoundingMode.HALF_UP),
                    caloriesPer100g = cal100.setScale(2, RoundingMode.HALF_UP),
                    proteinsPer100g = prot100.setScale(2, RoundingMode.HALF_UP),
                    fatsPer100g = fat100.setScale(2, RoundingMode.HALF_UP),
                    carbsPer100g = carb100.setScale(2, RoundingMode.HALF_UP),
                )
            recipe.ingredients.add(item)
        }

        recipe.totalWeightG = totalWeight.setScale(2, RoundingMode.HALF_UP)
        if (totalWeight > BigDecimal.ZERO) {
            val per100Factor = BigDecimal.valueOf(100).divide(totalWeight, 6, RoundingMode.HALF_UP)
            recipe.caloriesPer100g = totalCalories.multiply(per100Factor).setScale(2, RoundingMode.HALF_UP)
            recipe.proteinsPer100g = totalProteins.multiply(per100Factor).setScale(2, RoundingMode.HALF_UP)
            recipe.fatsPer100g = totalFats.multiply(per100Factor).setScale(2, RoundingMode.HALF_UP)
            recipe.carbsPer100g = totalCarbs.multiply(per100Factor).setScale(2, RoundingMode.HALF_UP)
        } else {
            recipe.caloriesPer100g = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            recipe.proteinsPer100g = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            recipe.fatsPer100g = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            recipe.carbsPer100g = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
    }
}
