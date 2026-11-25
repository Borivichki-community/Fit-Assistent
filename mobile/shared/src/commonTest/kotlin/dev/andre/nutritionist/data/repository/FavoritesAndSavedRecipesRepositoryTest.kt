package dev.andre.nutritionist.data.repository

import app.cash.turbine.test
import dev.andre.nutritionist.data.mapper.SavedRecipeEntityMapper.decodeIngredientsJson
import dev.andre.nutritionist.data.mapper.SavedRecipeEntityMapper.encodeIngredientsJson
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RecipeIngredient
import dev.andre.nutritionist.domain.model.RecipeIngredientId
import dev.andre.nutritionist.domain.model.RegionalPrice
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.fake.FakeCatalogRemoteDataSource
import dev.andre.nutritionist.fake.FakeFavoriteProductDao
import dev.andre.nutritionist.fake.FakeRecipeRemoteDataSource
import dev.andre.nutritionist.fake.FakeSavedRecipeDao
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesAndSavedRecipesRepositoryTest {

    private lateinit var favoriteProductDao: FakeFavoriteProductDao
    private lateinit var catalogRemote: FakeCatalogRemoteDataSource
    private lateinit var catalogRepository: CatalogRepositoryImpl

    private lateinit var savedRecipeDao: FakeSavedRecipeDao
    private lateinit var recipeRemote: FakeRecipeRemoteDataSource
    private lateinit var recipeRepository: RecipeRepositoryImpl

    @BeforeTest
    fun setUp() {
        val dispatchers = TestCoroutineDispatchers()
        favoriteProductDao = FakeFavoriteProductDao()
        catalogRemote = FakeCatalogRemoteDataSource()
        catalogRepository = CatalogRepositoryImpl(
            remoteDataSource = catalogRemote,
            favoriteProductDao = favoriteProductDao,
            dispatchers = dispatchers,
        )

        savedRecipeDao = FakeSavedRecipeDao()
        recipeRemote = FakeRecipeRemoteDataSource()
        recipeRepository = RecipeRepositoryImpl(
            remoteDataSource = recipeRemote,
            savedRecipeDao = savedRecipeDao,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `toggleFavoriteProduct adds removes and emits via observeFavoriteProducts`() = runTest {
        val product = Product(
            id = ProductId("prod-1"),
            name = "Творог 5%",
            category = "Молочные",
            nutritionPer100g = NutritionFacts(calories = 121.0, proteins = 17.2, fats = 5.0, carbs = 1.8),
            prices = listOf(
                RegionalPrice(
                    id = "rp-1",
                    productId = ProductId("prod-1"),
                    regionId = RegionId("ru-msk"),
                    price = Price(amount = Money.fromMinor(8_900L)),
                ),
            ),
        )

        catalogRepository.observeFavoriteProducts().test {
            assertTrue(awaitItem().isEmpty())

            val added = catalogRepository.toggleFavoriteProduct(product)
            assertTrue(added.isSuccess)
            assertTrue(added.getOrThrow())

            val listAfterAdd = awaitItem()
            assertEquals(1, listAfterAdd.size)
            assertEquals("Творог 5%", listAfterAdd.first().name)
            assertEquals(8_900L, listAfterAdd.first().prices.first().price.amount.amountMinor)

            val removed = catalogRepository.toggleFavoriteProduct(product)
            assertTrue(removed.isSuccess)
            assertFalse(removed.getOrThrow())
            assertTrue(awaitItem().isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveRecipe and deleteSavedRecipe persist and emit via observeSavedRecipes`() = runTest {
        val recipe = Recipe(
            id = RecipeId("rec-1"),
            title = "Омлет с сыром \"Гауда\"\\nи зеленью",
            mealType = MealType.BREAKFAST,
            nutrition = NutritionFacts(calories = 320.0, proteins = 22.0, fats = 24.0, carbs = 3.0),
            estimatedCost = Money.fromMinor(9_500L),
            ingredients = listOf(
                RecipeIngredient(
                    id = RecipeIngredientId("ing-1"),
                    productId = ProductId("p-egg"),
                    name = "Яйцо куриное",
                    weightGrams = 120.0,
                    nutritionPer100g = NutritionFacts(calories = 155.0, proteins = 13.0, fats = 11.0, carbs = 1.1),
                    cost = Money.fromMinor(4_500L),
                ),
                RecipeIngredient(
                    id = RecipeIngredientId("ing-2"),
                    productId = null,
                    name = "Сыр \"Гауда\"",
                    weightGrams = 30.0,
                    nutritionPer100g = NutritionFacts(calories = 356.0, proteins = 25.0, fats = 27.0, carbs = 2.2),
                    cost = null,
                ),
            ),
            instructions = "Взбить яйца и обжарить.",
        )

        recipeRepository.observeSavedRecipes().test {
            assertTrue(awaitItem().isEmpty())

            assertTrue(recipeRepository.saveRecipe(recipe).isSuccess)
            val savedList = awaitItem()
            assertEquals(1, savedList.size)
            val restored = savedList.first()
            assertEquals(recipe.title, restored.title)
            assertEquals(2, restored.ingredients.size)
            assertEquals("Сыр \"Гауда\"", restored.ingredients[1].name)
            assertEquals(4_500L, restored.ingredients[0].cost?.amountMinor)

            assertTrue(recipeRepository.deleteSavedRecipe(recipe.id).isSuccess)
            assertTrue(awaitItem().isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SavedRecipeEntityMapper ingredients JSON round-trip preserves special characters`() {
        val ingredients = listOf(
            RecipeIngredient(
                id = RecipeIngredientId("i-1"),
                productId = ProductId("p-1"),
                name = "Молоко 3.2% \"Домик\"\tв деревне",
                weightGrams = 250.5,
                nutritionPer100g = NutritionFacts(calories = 60.0, proteins = 3.0, fats = 3.2, carbs = 4.7),
                cost = Money.fromMinor(3_200L),
            ),
        )

        val encoded = encodeIngredientsJson(ingredients)
        val decoded = decodeIngredientsJson(encoded)

        assertEquals(1, decoded.size)
        assertEquals(ingredients.first(), decoded.first())
    }
}
