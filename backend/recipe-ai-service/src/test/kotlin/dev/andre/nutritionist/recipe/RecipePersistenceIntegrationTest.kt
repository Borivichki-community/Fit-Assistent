package dev.andre.nutritionist.recipe

import dev.andre.nutritionist.recipe.persistence.entity.AccessGrantEntity
import dev.andre.nutritionist.recipe.persistence.entity.CoachStudentLinkEntity
import dev.andre.nutritionist.recipe.persistence.entity.MealPlanEntity
import dev.andre.nutritionist.recipe.persistence.entity.MealPlanItemEntity
import dev.andre.nutritionist.recipe.persistence.entity.RecipeEntity
import dev.andre.nutritionist.recipe.persistence.entity.RecipeIngredientEntity
import dev.andre.nutritionist.recipe.persistence.repository.AccessGrantRepository
import dev.andre.nutritionist.recipe.persistence.repository.CoachStudentLinkRepository
import dev.andre.nutritionist.recipe.persistence.repository.MealPlanItemRepository
import dev.andre.nutritionist.recipe.persistence.repository.MealPlanRepository
import dev.andre.nutritionist.recipe.persistence.repository.RecipeIngredientRepository
import dev.andre.nutritionist.recipe.persistence.repository.RecipeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class RecipePersistenceIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var recipeRepository: RecipeRepository

    @Autowired
    private lateinit var recipeIngredientRepository: RecipeIngredientRepository

    @Autowired
    private lateinit var mealPlanRepository: MealPlanRepository

    @Autowired
    private lateinit var mealPlanItemRepository: MealPlanItemRepository

    @Autowired
    private lateinit var coachStudentLinkRepository: CoachStudentLinkRepository

    @Autowired
    private lateinit var accessGrantRepository: AccessGrantRepository

    @Test
    fun `seed system recipes are loaded by liquibase and match catalog product ids`() {
        val systemRecipes = recipeRepository.findByAuthorType("SYSTEM")
        assertThat(systemRecipes).hasSize(15)

        systemRecipes.forEach { recipe ->
            assertThat(recipe.visibility).isEqualTo("PUBLIC")
            assertThat(recipe.authorType).isEqualTo("SYSTEM")
            assertThat(recipe.title).isNotBlank()
            assertThat(recipe.totalWeightG).isGreaterThan(BigDecimal.ZERO)
            assertThat(recipe.caloriesPer100g).isGreaterThan(BigDecimal.ZERO)

            val ingredients = recipeIngredientRepository.findByRecipeId(recipe.id)
            assertThat(ingredients).isNotEmpty
            ingredients.forEach { ingredient ->
                assertThat(ingredient.productId).isNotNull
                assertThat(ingredient.productNameSnapshot).isNotBlank()
                assertThat(ingredient.weightGrams).isGreaterThan(BigDecimal.ZERO)
            }
        }

        // Verify specific known seed recipes and product UUIDs from catalog
        val buckwheatRecipe =
            systemRecipes.firstOrNull { it.id == UUID.fromString("10000000-0000-0000-0000-000000000001") }
        assertThat(buckwheatRecipe).isNotNull
        assertThat(buckwheatRecipe?.title).isEqualTo("Гречневая каша с куриным филе")

        val buckwheatIngredients = recipeIngredientRepository.findByRecipeId(buckwheatRecipe!!.id)
        assertThat(buckwheatIngredients).hasSize(3)

        val productIds = buckwheatIngredients.map { it.productId }
        assertThat(productIds).containsExactlyInAnyOrder(
            UUID.fromString("20000000-0000-0000-0000-000000000001"), // Гречневая крупа ядрица
            UUID.fromString("20000000-0000-0000-0000-000000000057"), // Куриное филе грудки охл
            UUID.fromString("20000000-0000-0000-0000-000000000043"), // Масло сливочное Традиционное 82.5%
        )
    }

    @Test
    @Transactional
    fun `save and read recipe with ingredients and origin self reference`() {
        val baseRecipe =
            RecipeEntity(
                id = UUID.randomUUID(),
                authorType = "USER",
                authorId = "usr_chef_1",
                title = "Базовый омлет",
                description = "Простой базовый рецепт",
                visibility = "PUBLIC",
                servingsCount = 1,
                totalWeightG = BigDecimal("200.00"),
                caloriesPer100g = BigDecimal("120.00"),
                proteinsPer100g = BigDecimal("10.00"),
                fatsPer100g = BigDecimal("8.00"),
                carbsPer100g = BigDecimal("2.00"),
            )
        val savedBase = recipeRepository.saveAndFlush(baseRecipe)

        val forkedRecipe =
            RecipeEntity(
                id = UUID.randomUUID(),
                authorType = "USER",
                authorId = "usr_chef_2",
                originRecipe = savedBase,
                title = "Омлет с добавками",
                description = "Форк базового омлета",
                visibility = "PRIVATE",
                servingsCount = 1,
                totalWeightG = BigDecimal("250.00"),
                caloriesPer100g = BigDecimal("140.00"),
                proteinsPer100g = BigDecimal("12.00"),
                fatsPer100g = BigDecimal("9.00"),
                carbsPer100g = BigDecimal("3.00"),
            )
        forkedRecipe.addIngredient(
            RecipeIngredientEntity(
                id = UUID.randomUUID(),
                productId = UUID.fromString("20000000-0000-0000-0000-000000000065"),
                productNameSnapshot = "Яйцо куриное столовое С0",
                weightGrams = BigDecimal("120.00"),
                caloriesPer100g = BigDecimal("157.00"),
                proteinsPer100g = BigDecimal("12.70"),
                fatsPer100g = BigDecimal("11.50"),
                carbsPer100g = BigDecimal("0.70"),
            ),
        )
        forkedRecipe.addIngredient(
            RecipeIngredientEntity(
                id = UUID.randomUUID(),
                productId = UUID.fromString("20000000-0000-0000-0000-000000000036"),
                productNameSnapshot = "Сыр Российский 50%",
                weightGrams = BigDecimal("30.00"),
                caloriesPer100g = BigDecimal("364.00"),
                proteinsPer100g = BigDecimal("23.00"),
                fatsPer100g = BigDecimal("30.00"),
                carbsPer100g = BigDecimal("0.00"),
            ),
        )

        val savedForked = recipeRepository.saveAndFlush(forkedRecipe)
        assertThat(savedForked.version).isEqualTo(0L)

        val found = recipeRepository.findWithIngredientsById(savedForked.id)
        assertThat(found).isNotNull
        assertThat(found?.title).isEqualTo("Омлет с добавками")
        assertThat(found?.originRecipe?.id).isEqualTo(savedBase.id)
        assertThat(found?.ingredients).hasSize(2)
    }

    @Test
    fun `cascade delete recipe removes its ingredients`() {
        val recipe =
            RecipeEntity(
                id = UUID.randomUUID(),
                authorType = "AI",
                authorId = "ai_agent",
                title = "Рецепт для каскадного удаления",
                visibility = "PRIVATE",
                servingsCount = 1,
                totalWeightG = BigDecimal("100.00"),
                caloriesPer100g = BigDecimal("100.00"),
                proteinsPer100g = BigDecimal("10.00"),
                fatsPer100g = BigDecimal("5.00"),
                carbsPer100g = BigDecimal("5.00"),
            )
        val ingId1 = UUID.randomUUID()
        val ingId2 = UUID.randomUUID()
        recipe.addIngredient(
            RecipeIngredientEntity(
                id = ingId1,
                productNameSnapshot = "Ингредиент 1",
                weightGrams = BigDecimal("50.00"),
            ),
        )
        recipe.addIngredient(
            RecipeIngredientEntity(
                id = ingId2,
                productNameSnapshot = "Ингредиент 2",
                weightGrams = BigDecimal("50.00"),
            ),
        )

        val saved = recipeRepository.saveAndFlush(recipe)
        assertThat(recipeIngredientRepository.findByRecipeId(saved.id)).hasSize(2)

        recipeRepository.deleteById(saved.id)
        recipeRepository.flush()

        assertThat(recipeRepository.findById(saved.id)).isEmpty
        assertThat(recipeIngredientRepository.findById(ingId1)).isEmpty
        assertThat(recipeIngredientRepository.findById(ingId2)).isEmpty
    }

    @Test
    fun `optimistic locking version increments on update`() {
        val recipe =
            RecipeEntity(
                id = UUID.randomUUID(),
                authorType = "USER",
                authorId = "usr_version_test",
                title = "Исходный заголовок",
                visibility = "PRIVATE",
                servingsCount = 1,
                totalWeightG = BigDecimal("100.00"),
                caloriesPer100g = BigDecimal("100.00"),
                proteinsPer100g = BigDecimal("10.00"),
                fatsPer100g = BigDecimal("5.00"),
                carbsPer100g = BigDecimal("5.00"),
            )
        val saved = recipeRepository.saveAndFlush(recipe)
        assertThat(saved.version).isEqualTo(0L)

        saved.title = "Обновленный заголовок"
        saved.servingsCount = 2
        val updated = recipeRepository.saveAndFlush(saved)

        assertThat(updated.version).isEqualTo(1L)
        assertThat(updated.title).isEqualTo("Обновленный заголовок")
        assertThat(updated.servingsCount).isEqualTo(2)
    }

    @Test
    @Transactional
    fun `save meal plan with items and cascade delete`() {
        val plan =
            MealPlanEntity(
                id = UUID.randomUUID(),
                authorId = "coach_42",
                authorType = "COACH",
                title = "Недельный план сушки",
                description = "Рацион с дефицитом калорий",
                durationDays = 7,
                visibility = "PRIVATE",
                accessMode = "PAID",
                priceAmountMinor = 149000L,
                currency = "RUB",
                avgDailyCalories = BigDecimal("1850.00"),
                avgDailyProteins = BigDecimal("140.00"),
                avgDailyFats = BigDecimal("55.00"),
                avgDailyCarbs = BigDecimal("180.00"),
            )
        val itemId1 = UUID.randomUUID()
        val itemId2 = UUID.randomUUID()
        plan.addItem(
            MealPlanItemEntity(
                id = itemId1,
                dayNumber = 1,
                mealSlot = "BREAKFAST",
                itemType = "RECIPE",
                itemId = UUID.randomUUID(),
                nameSnapshot = "Овсяная каша",
                weightGrams = BigDecimal("250.00"),
                calories = BigDecimal("320.00"),
                proteins = BigDecimal("12.00"),
                fats = BigDecimal("6.00"),
                carbs = BigDecimal("54.00"),
            ),
        )
        plan.addItem(
            MealPlanItemEntity(
                id = itemId2,
                dayNumber = 1,
                mealSlot = "LUNCH",
                itemType = "RECIPE",
                itemId = UUID.randomUUID(),
                nameSnapshot = "Куриная грудка с рисом",
                weightGrams = BigDecimal("350.00"),
                calories = BigDecimal("520.00"),
                proteins = BigDecimal("45.00"),
                fats = BigDecimal("8.00"),
                carbs = BigDecimal("65.00"),
            ),
        )

        val savedPlan = mealPlanRepository.saveAndFlush(plan)
        assertThat(savedPlan.version).isEqualTo(0L)

        val found = mealPlanRepository.findWithItemsById(savedPlan.id)
        assertThat(found).isNotNull
        assertThat(found?.title).isEqualTo("Недельный план сушки")
        assertThat(found?.priceAmountMinor).isEqualTo(149000L)
        assertThat(found?.items).hasSize(2)

        // Cascade delete
        mealPlanRepository.deleteById(savedPlan.id)
        mealPlanRepository.flush()

        assertThat(mealPlanRepository.findById(savedPlan.id)).isEmpty
        assertThat(mealPlanItemRepository.findById(itemId1)).isEmpty
        assertThat(mealPlanItemRepository.findById(itemId2)).isEmpty
    }

    @Test
    fun `coach student links and access grants persistence and queries`() {
        val coachId = "coach_smith_${UUID.randomUUID()}"
        val studentId = "student_doe_${UUID.randomUUID()}"

        val link =
            CoachStudentLinkEntity(
                coachId = coachId,
                studentId = studentId,
                status = "ACTIVE",
                canViewDiary = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        val savedLink = coachStudentLinkRepository.saveAndFlush(link)
        assertThat(savedLink.id).isNotNull

        val foundLink = coachStudentLinkRepository.findByCoachIdAndStudentId(coachId, studentId)
        assertThat(foundLink).isNotNull
        assertThat(foundLink?.status).isEqualTo("ACTIVE")
        assertThat(foundLink?.canViewDiary).isTrue()

        // Access grant
        val targetId = UUID.randomUUID()
        val grant =
            AccessGrantEntity(
                userId = studentId,
                targetType = "MEAL_PLAN",
                targetId = targetId,
                grantSource = "COACH_ASSIGNMENT",
                grantedBy = coachId,
                expiresAt = Instant.now().plusSeconds(86400 * 30),
            )
        val savedGrant = accessGrantRepository.saveAndFlush(grant)
        assertThat(savedGrant.id).isNotNull

        val foundGrant =
            accessGrantRepository.findByUserIdAndTargetTypeAndTargetId(
                userId = studentId,
                targetType = "MEAL_PLAN",
                targetId = targetId,
            )
        assertThat(foundGrant).isNotNull
        assertThat(foundGrant?.grantSource).isEqualTo("COACH_ASSIGNMENT")
        assertThat(foundGrant?.grantedBy).isEqualTo(coachId)
    }
}
