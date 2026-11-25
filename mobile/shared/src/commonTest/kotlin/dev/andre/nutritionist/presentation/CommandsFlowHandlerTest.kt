package dev.andre.nutritionist.presentation

import app.cash.turbine.test
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealPlan
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.Product
import dev.andre.nutritionist.domain.model.ProductId
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.TargetPeriod
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuParams
import dev.andre.nutritionist.domain.usecase.GenerateBudgetMenuUseCase
import dev.andre.nutritionist.domain.usecase.MenuGenerationProgress
import dev.andre.nutritionist.fake.FakeCatalogRepository
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.FakeRecipeRepository
import dev.andre.nutritionist.presentation.airecipe.MenuCommand
import dev.andre.nutritionist.presentation.airecipe.MenuCommandsFlowHandler
import dev.andre.nutritionist.presentation.airecipe.MenuEvent
import dev.andre.nutritionist.presentation.catalog.CatalogSearchCommand
import dev.andre.nutritionist.presentation.catalog.CatalogSearchCommandsFlowHandler
import dev.andre.nutritionist.presentation.catalog.CatalogSearchEvent
import dev.andre.nutritionist.presentation.dashboard.DiaryCommand
import dev.andre.nutritionist.presentation.dashboard.DiaryCommandsFlowHandler
import dev.andre.nutritionist.presentation.dashboard.DiaryEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CommandsFlowHandlerTest {

    private fun testDispatchers(dispatcher: CoroutineDispatcher): CoroutineDispatchers =
        object : CoroutineDispatchers {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val unconfined: CoroutineDispatcher = dispatcher
        }

    @Test
    fun `CatalogSearchCommandsFlowHandler applies 300ms debounce and distinctUntilChanged`() {
        val testDispatcher = StandardTestDispatcher()
        runTest(testDispatcher) {
            val catalogRepo = FakeCatalogRepository()
            val apple = Product(
                id = ProductId("p-1"),
                name = "Яблоко",
                category = "Фрукты",
                nutritionPer100g = NutritionFacts(calories = 52.0, proteins = 0.3, fats = 0.2, carbs = 14.0),
            )
            catalogRepo.products[apple.id] = apple

            val handler = CatalogSearchCommandsFlowHandler(
                catalogRepository = catalogRepo,
                dispatchers = testDispatchers(testDispatcher),
                debounceMillis = 300L,
            )
            val commands = MutableSharedFlow<CatalogSearchCommand>()

            handler.handle(commands).test {
                runCurrent()
                commands.emit(CatalogSearchCommand.Search(query = "яб", category = null))
                advanceTimeBy(150.milliseconds)
                runCurrent()
                expectNoEvents()
                assertEquals(0, catalogRepo.searchCallCount)

                // Rapid follow-up query cancels first debounce window
                commands.emit(CatalogSearchCommand.Search(query = "яблоко", category = null))
                advanceTimeBy(299.milliseconds)
                runCurrent()
                expectNoEvents()

                advanceTimeBy(2.milliseconds)
                runCurrent()
                val loaded = assertIs<CatalogSearchEvent.Internal.Loaded>(awaitItem())
                assertEquals(1, loaded.products.size)
                assertEquals(1, catalogRepo.searchCallCount)

                // Duplicate search command is filtered out by distinctUntilChanged
                commands.emit(CatalogSearchCommand.Search(query = "яблоко", category = null))
                advanceTimeBy(400.milliseconds)
                runCurrent()
                expectNoEvents()
                assertEquals(1, catalogRepo.searchCallCount)

                // forceRefresh bypasses distinctUntilChanged and debounce
                commands.emit(
                    CatalogSearchCommand.Search(
                        query = "яблоко",
                        category = null,
                        forceRefresh = true,
                    ),
                )
                runCurrent()
                assertIs<CatalogSearchEvent.Internal.Loaded>(awaitItem())
                assertEquals(2, catalogRepo.searchCallCount)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `MenuCommandsFlowHandler cancels previous generation via collectLatest`() {
        val testDispatcher = StandardTestDispatcher()
        runTest(testDispatcher) {
            val recipeRepo = FakeRecipeRepository()
            val catalogRepo = FakeCatalogRepository()
            val dispatchers = testDispatchers(testDispatcher)
            val useCase = GenerateBudgetMenuUseCase(
                recipeRepository = recipeRepo,
                catalogRepository = catalogRepo,
                dispatchers = dispatchers,
            )
            val handler = MenuCommandsFlowHandler(
                generateBudgetMenuUseCase = useCase,
                dispatchers = dispatchers,
            )

            recipeRepo.generateDelayMs = 1_000L
            recipeRepo.planToReturn = MealPlan(
                id = "final-plan",
                period = TargetPeriod.DAY,
                meals = emptyList(),
                totalNutrition = NutritionFacts(calories = 2000.0),
                estimatedCost = Money.fromRubles(400.0),
            )

            val commands = MutableSharedFlow<MenuCommand>()
            val firstParams = GenerateBudgetMenuParams(
                period = TargetPeriod.DAY,
                targetCalories = 1800.0,
                maxBudget = Money.fromRubles(400.0),
            )
            val secondParams = firstParams.copy(targetCalories = 2200.0)

            handler.handle(commands).test {
                runCurrent()
                commands.emit(MenuCommand.GenerateMenu(firstParams))
                advanceTimeBy(100.milliseconds)
                runCurrent()
                val firstInit = assertIs<MenuEvent.Internal.ProgressUpdated>(awaitItem())
                assertEquals(MenuGenerationProgress.Initializing, firstInit.progress)

                // New GenerateMenu command cancels first suspended coroutine via collectLatest
                commands.emit(MenuCommand.GenerateMenu(secondParams))
                advanceTimeBy(100.milliseconds)
                runCurrent()
                val secondInit = assertIs<MenuEvent.Internal.ProgressUpdated>(awaitItem())
                assertEquals(MenuGenerationProgress.Initializing, secondInit.progress)

                advanceUntilIdle()
                val completed = assertIs<MenuEvent.Internal.GenerationCompleted>(awaitItem())
                assertEquals("final-plan", completed.plan.id)
                assertEquals(1, recipeRepo.generateCancelledCount)
                assertEquals(2, recipeRepo.generateCallCount)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `DiaryCommandsFlowHandler observes records reactively and recalculates daily balance`() {
        val testDispatcher = StandardTestDispatcher()
        runTest(testDispatcher) {
            val diaryRepo = FakeDiaryRepository()
            val dispatchers = testDispatchers(testDispatcher)
            val handler = DiaryCommandsFlowHandler(
                diaryRepository = diaryRepo,
                dispatchers = dispatchers,
                timeZone = TimeZone.UTC,
            )

            val dayMs = Instant.parse("2026-11-24T10:00:00Z").toEpochMilliseconds()
            val commands = MutableSharedFlow<DiaryCommand>()

            handler.handle(commands).test {
                runCurrent()
                commands.emit(DiaryCommand.ObserveDate(dayMs))
                runCurrent()

                val initial = assertIs<DiaryEvent.Internal.RecordsLoaded>(awaitItem())
                assertEquals(0, initial.records.size)
                assertEquals(0, initial.balance.consumedCalories)

                diaryRepo.addEntry(
                    MealRecord(
                        id = MealRecordId("rec-1"),
                        mealType = MealType.LUNCH,
                        recordedAt = Instant.fromEpochMilliseconds(dayMs),
                        sourceType = EntrySourceType.PRODUCT,
                        nameSnapshot = "Куриная грудка",
                        weightGrams = 200.0,
                        nutrition = NutritionFacts(calories = 330.0, proteins = 62.0, fats = 7.0, carbs = 0.0),
                        cost = Money.fromRubles(120.0),
                        idempotencyKey = IdempotencyKey("idem-diary-1"),
                        syncStatus = SyncStatus.PENDING,
                    ),
                )
                runCurrent()

                val updated = assertIs<DiaryEvent.Internal.RecordsLoaded>(awaitItem())
                assertEquals(1, updated.records.size)
                assertEquals(330, updated.balance.consumedCalories)
                assertEquals(62.0, updated.balance.proteins.consumedGrams)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
