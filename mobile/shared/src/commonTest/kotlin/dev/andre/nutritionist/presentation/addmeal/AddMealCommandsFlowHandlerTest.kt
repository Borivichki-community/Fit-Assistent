package dev.andre.nutritionist.presentation.addmeal

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.ContentVisibility
import dev.andre.nutritionist.domain.model.EntrySourceType
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
import dev.andre.nutritionist.domain.usecase.LogRecipeToDiaryUseCase
import dev.andre.nutritionist.fake.FakeCatalogRepository
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.FakeRecipeCatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AddMealCommandsFlowHandlerTest {

    private fun testDispatchers(dispatcher: CoroutineDispatcher): CoroutineDispatchers =
        object : CoroutineDispatchers {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val unconfined: CoroutineDispatcher = dispatcher
        }

    private fun epicaProduct() = Product(
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

    private fun yogurtBowlRecipe() = Recipe(
        id = RecipeId("10000000-0000-0000-0000-000000000010"),
        title = "Йогуртовый боул с ягодами",
        authorType = RecipeAuthorType.SYSTEM,
        visibility = ContentVisibility.PUBLIC,
        servingsCount = 1,
        totalWeightGrams = 250.0,
        nutrition = NutritionFacts(calories = 92.0, proteins = 5.4, fats = 2.0, carbs = 13.0),
    )

    @Test
    fun `search queries both catalog and recipe microservices with debounce`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val catalogRepo = FakeCatalogRepository().apply {
                val p = epicaProduct()
                products[p.id] = p
            }
            val recipeRepo = FakeRecipeCatalogRepository().apply {
                publicRecipes.add(yogurtBowlRecipe())
            }
            val diaryRepo = FakeDiaryRepository()
            val dispatchers = testDispatchers(dispatcher)
            val handler = AddMealCommandsFlowHandler(
                catalogRepository = catalogRepo,
                recipeCatalogRepository = recipeRepo,
                diaryRepository = diaryRepo,
                logRecipeToDiaryUseCase = LogRecipeToDiaryUseCase(diaryRepo, dispatchers),
                dispatchers = dispatchers,
                debounceMillis = 300L,
                timeZone = TimeZone.UTC,
            )

            val commands = MutableSharedFlow<AddMealCommand>()
            handler.handle(commands).test {
                runCurrent()
                commands.emit(AddMealCommand.Search("йог"))
                advanceTimeBy(150.milliseconds)
                runCurrent()
                expectNoEvents()

                commands.emit(AddMealCommand.Search("йогурт"))
                advanceTimeBy(301.milliseconds)
                runCurrent()

                val loaded = assertIs<AddMealEvent.Internal.SearchLoaded>(awaitItem())
                assertEquals("йогурт", loaded.query)
                assertEquals(1, loaded.products.size)
                assertEquals("Йогурт Epica", loaded.products.first().name)
                assertEquals(1, loaded.recipes.size)
                assertEquals("Йогуртовый боул с ягодами", loaded.recipes.first().title)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `search still returns catalog products when recipe service fails`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val catalogRepo = FakeCatalogRepository().apply {
                val p = epicaProduct()
                products[p.id] = p
            }
            val recipeRepo = FakeRecipeCatalogRepository().apply {
                shouldThrow = RuntimeException("recipe-ai-service unavailable")
            }
            val diaryRepo = FakeDiaryRepository()
            val dispatchers = testDispatchers(dispatcher)
            val handler = AddMealCommandsFlowHandler(
                catalogRepository = catalogRepo,
                recipeCatalogRepository = recipeRepo,
                diaryRepository = diaryRepo,
                logRecipeToDiaryUseCase = LogRecipeToDiaryUseCase(diaryRepo, dispatchers),
                dispatchers = dispatchers,
                debounceMillis = 0L,
                timeZone = TimeZone.UTC,
            )

            val commands = MutableSharedFlow<AddMealCommand>()
            handler.handle(commands).test {
                runCurrent()
                commands.emit(AddMealCommand.Search("epica", forceRefresh = true))
                runCurrent()

                val loaded = assertIs<AddMealEvent.Internal.SearchLoaded>(awaitItem())
                assertEquals(1, loaded.products.size)
                assertEquals(0, loaded.recipes.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `AddProduct and AddRecipe persist records into selected mealType and date`() {
        val dispatcher = StandardTestDispatcher()
        runTest(dispatcher) {
            val catalogRepo = FakeCatalogRepository()
            val recipeRepo = FakeRecipeCatalogRepository()
            val diaryRepo = FakeDiaryRepository()
            val dispatchers = testDispatchers(dispatcher)
            val todayMs = Instant.parse("2026-11-26T15:00:00Z").toEpochMilliseconds()
            val selectedOtherDayMs = Instant.parse("2026-11-24T12:00:00Z").toEpochMilliseconds()

            val handler = AddMealCommandsFlowHandler(
                catalogRepository = catalogRepo,
                recipeCatalogRepository = recipeRepo,
                diaryRepository = diaryRepo,
                logRecipeToDiaryUseCase = LogRecipeToDiaryUseCase(diaryRepo, dispatchers),
                dispatchers = dispatchers,
                debounceMillis = 0L,
                nowProvider = { todayMs },
                timeZone = TimeZone.UTC,
            )

            val commands = MutableSharedFlow<AddMealCommand>()
            handler.handle(commands).test {
                runCurrent()

                commands.emit(
                    AddMealCommand.AddProduct(
                        product = epicaProduct(),
                        mealType = MealType.BREAKFAST,
                        dateEpochMs = selectedOtherDayMs,
                    ),
                )
                runCurrent()

                val productSaved = assertIs<AddMealEvent.Internal.ItemSaved>(awaitItem())
                assertEquals(MealType.BREAKFAST, productSaved.record.mealType)
                assertEquals(EntrySourceType.PRODUCT, productSaved.record.sourceType)
                assertEquals(selectedOtherDayMs, productSaved.record.recordedAt.toEpochMilliseconds())
                assertEquals(Money.fromMinor(9500L), productSaved.record.cost)
                assertEquals(65.38, productSaved.record.nutrition.calories)

                commands.emit(
                    AddMealCommand.AddRecipe(
                        recipe = yogurtBowlRecipe(),
                        mealType = MealType.DINNER,
                        dateEpochMs = todayMs,
                    ),
                )
                runCurrent()

                val recipeSaved = assertIs<AddMealEvent.Internal.ItemSaved>(awaitItem())
                assertEquals(MealType.DINNER, recipeSaved.record.mealType)
                assertEquals(EntrySourceType.RECIPE, recipeSaved.record.sourceType)
                assertEquals(todayMs, recipeSaved.record.recordedAt.toEpochMilliseconds())
                assertEquals(250.0, recipeSaved.record.weightGrams)
                assertEquals(230.0, recipeSaved.record.nutrition.calories)

                assertEquals(2, diaryRepo.addedRecords.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
