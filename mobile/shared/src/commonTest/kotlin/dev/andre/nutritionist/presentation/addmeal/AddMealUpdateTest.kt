package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Price
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.Recipe
import dev.andre.nutritionist.domain.model.RecipeAuthorType
import dev.andre.nutritionist.domain.model.RecipeId
import dev.andre.nutritionist.domain.model.RegionId
import dev.andre.nutritionist.domain.model.RegionalPrice
import dev.andre.nutritionist.domain.model.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class AddMealUpdateTest {

    private val update = AddMealUpdate()
    private val mapper = AddMealUiStateMapper()

    private fun sampleProduct() = Product(
        id = ProductId("20000000-0000-0000-0000-000000000112"),
        name = "Йогурт Epica",
        category = "DAIRY",
        nutritionPer100g = NutritionFacts(calories = 65.38, proteins = 3.79, fats = 0.96, carbs = 10.62),
        prices = listOf(
            RegionalPrice(
                id = "rp-1",
                productId = ProductId("20000000-0000-0000-0000-000000000112"),
                regionId = RegionId("reg-1"),
                price = Price(amount = Money.fromMinor(9500L), unit = "UNIT"),
                regionName = "Москва",
            ),
        ),
    )

    private fun sampleRecipe() = Recipe(
        id = RecipeId("10000000-0000-0000-0000-000000000001"),
        title = "Гречневая каша с куриным филе",
        authorType = RecipeAuthorType.SYSTEM,
        visibility = ContentVisibility.PUBLIC,
        servingsCount = 1,
        totalWeightGrams = 365.0,
        nutrition = NutritionFacts(calories = 221.29, proteins = 18.13, fats = 5.79, carbs = 25.77),
    )

    @Test
    fun `Open resets state to Idle with target mealType and date`() {
        val prev = AddMealState(query = "йогурт", content = AddMealState.Content.Empty)
        val next = update.update(
            prev,
            AddMealEvent.Ui.Open(mealType = MealType.DINNER, dateEpochMs = 1_794_000_000_000L),
        )
        val state = requireNotNull(next.state)
        assertEquals(MealType.DINNER, state.mealType)
        assertEquals(1_794_000_000_000L, state.selectedDateEpochMs)
        assertEquals("", state.query)
        assertIs<AddMealState.Content.Idle>(state.content)
        assertEquals("Вечерний приём", mapper.map(state).mealTypeTitle)
    }

    @Test
    fun `QueryChanged sets Loading and emits Search command when non-blank, Idle when blank`() {
        val searchNext = update.update(AddMealState(), AddMealEvent.Ui.QueryChanged("  йогурт "))
        assertIs<AddMealState.Content.Loading>(searchNext.state?.content)
        assertEquals(listOf(AddMealCommand.Search(query = "йогурт")), searchNext.commands)

        val blankNext = update.update(
            requireNotNull(searchNext.state),
            AddMealEvent.Ui.QueryChanged("   "),
        )
        assertIs<AddMealState.Content.Idle>(blankNext.state?.content)
        assertTrue(blankNext.commands.isEmpty())
    }

    @Test
    fun `SearchLoaded maps products and recipes and ignores stale query`() {
        val searching = AddMealState(query = "греч", content = AddMealState.Content.Loading)
        val stale = update.update(
            searching,
            AddMealEvent.Internal.SearchLoaded(
                query = "йогурт",
                products = listOf(sampleProduct()),
                recipes = emptyList(),
            ),
        )
        assertEquals(null, stale.state)

        val current = update.update(
            searching.copy(query = "каша"),
            AddMealEvent.Internal.SearchLoaded(
                query = "каша",
                products = listOf(sampleProduct()),
                recipes = listOf(sampleRecipe()),
            ),
        )
        val loadedState = requireNotNull(current.state)
        val content = assertIs<AddMealState.Content.Success>(loadedState.content)
        assertEquals(false, content.isSaving)
        assertEquals(2, loadedState.items.size)
        assertEquals(AddMealItemKind.PRODUCT, loadedState.items[0].kind)
        assertEquals("Продукт", loadedState.items[0].typeLabel)
        assertEquals("95 ₽", loadedState.items[0].priceLabel)
        assertEquals(AddMealItemKind.RECIPE, loadedState.items[1].kind)
        assertEquals("Рецепт", loadedState.items[1].typeLabel)
        assertEquals("365 г", loadedState.items[1].portionLabel)
    }

    @Test
    fun `ItemClicked dispatches AddProduct or AddRecipe and ItemSaved emits MealAdded and Dismiss`() {
        val product = sampleProduct()
        val recipe = sampleRecipe()
        val baseState = AddMealState(
            mealType = MealType.BREAKFAST,
            selectedDateEpochMs = 1_794_050_000_000L,
            query = "еда",
            domainProducts = mapOf(product.id to product),
            domainRecipes = mapOf(recipe.id to recipe),
            content = AddMealState.Content.Success(isSaving = false),
        )

        val addProdNext = update.update(
            baseState,
            AddMealEvent.Ui.ItemClicked(id = product.id.raw, kind = AddMealItemKind.PRODUCT),
        )
        val savingContent = assertIs<AddMealState.Content.Success>(addProdNext.state?.content)
        assertTrue(savingContent.isSaving)
        assertEquals(
            listOf(
                AddMealCommand.AddProduct(
                    product = product,
                    mealType = MealType.BREAKFAST,
                    dateEpochMs = 1_794_050_000_000L,
                ),
            ),
            addProdNext.commands,
        )

        val addRecNext = update.update(
            baseState,
            AddMealEvent.Ui.ItemClicked(id = recipe.id.raw, kind = AddMealItemKind.RECIPE),
        )
        assertEquals(
            listOf(
                AddMealCommand.AddRecipe(
                    recipe = recipe,
                    mealType = MealType.BREAKFAST,
                    dateEpochMs = 1_794_050_000_000L,
                ),
            ),
            addRecNext.commands,
        )

        val savedRecord = MealRecord(
            id = MealRecordId("rec-1"),
            mealType = MealType.BREAKFAST,
            recordedAt = Instant.fromEpochMilliseconds(1_794_050_000_000L),
            sourceType = EntrySourceType.PRODUCT,
            sourceId = product.id.raw,
            nameSnapshot = product.name,
            weightGrams = 100.0,
            nutrition = NutritionFacts(calories = 65.38, proteins = 3.79, fats = 0.96, carbs = 10.62),
            cost = Money.fromMinor(9500L),
            idempotencyKey = IdempotencyKey("idem-1"),
            syncStatus = SyncStatus.PENDING,
        )
        val savedNext = update.update(baseState, AddMealEvent.Internal.ItemSaved(savedRecord))
        assertEquals(
            listOf(AddMealNews.MealAdded(savedRecord), AddMealNews.Dismiss),
            savedNext.news,
        )
    }

    @Test
    fun `stub buttons produce no commands and Dismiss emits Dismiss news`() {
        val ai = update.update(AddMealState(), AddMealEvent.Ui.AiGenerateClicked)
        val photo = update.update(AddMealState(), AddMealEvent.Ui.TakePhotoClicked)
        val voice = update.update(AddMealState(), AddMealEvent.Ui.VoiceAssistantClicked)
        val saved = update.update(AddMealState(), AddMealEvent.Ui.SavedFoodClicked)
        assertTrue(ai.commands.isEmpty() && photo.commands.isEmpty() && voice.commands.isEmpty() && saved.commands.isEmpty())

        val dismiss = update.update(AddMealState(), AddMealEvent.Ui.Dismiss)
        assertEquals(listOf(AddMealNews.Dismiss), dismiss.news)
    }
}
