package dev.andre.nutritionist.domain.usecase

import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.MealPlanDay
import dev.andre.nutritionist.domain.model.MealPlanItem
import dev.andre.nutritionist.domain.model.MealPlanItemId
import dev.andre.nutritionist.domain.model.MealPlanTemplate
import dev.andre.nutritionist.domain.model.MealPlanTemplateId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.PlanItemType
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import dev.andre.nutritionist.fake.FakeDiaryRepository
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ApplyMealPlanDayToDiaryUseCaseTest {

    private lateinit var diaryRepository: FakeDiaryRepository
    private lateinit var dispatchers: TestCoroutineDispatchers
    private lateinit var useCase: ApplyMealPlanDayToDiaryUseCase

    @BeforeTest
    fun setUp() {
        diaryRepository = FakeDiaryRepository()
        dispatchers = TestCoroutineDispatchers()
        useCase = ApplyMealPlanDayToDiaryUseCase(
            diaryRepository = diaryRepository,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `invoke batch logs all day items with proper source types and unique keys`() = runTest {
        val planId = MealPlanTemplateId("plan-1")
        val day = MealPlanDay(
            dayNumber = 1,
            items = listOf(
                MealPlanItem(
                    id = MealPlanItemId("item-1"),
                    planId = planId,
                    dayNumber = 1,
                    mealSlot = MealType.BREAKFAST,
                    itemType = PlanItemType.RECIPE,
                    itemId = "rec-oatmeal",
                    nameSnapshot = "Овсяная каша",
                    weightGrams = 250.0,
                    nutrition = NutritionFacts(calories = 310.0, proteins = 10.0, fats = 6.0, carbs = 52.0),
                    estimatedCost = Money.fromMinor(4500L),
                ),
                MealPlanItem(
                    id = MealPlanItemId("item-2"),
                    planId = planId,
                    dayNumber = 1,
                    mealSlot = MealType.SNACK,
                    itemType = PlanItemType.PRODUCT,
                    itemId = "prod-apple",
                    nameSnapshot = "Яблоко",
                    weightGrams = 180.0,
                    nutrition = NutritionFacts(calories = 94.0, proteins = 0.5, fats = 0.4, carbs = 21.0),
                    estimatedCost = null,
                ),
                MealPlanItem(
                    id = MealPlanItemId("item-3"),
                    planId = planId,
                    dayNumber = 1,
                    mealSlot = MealType.DINNER,
                    itemType = PlanItemType.UNSPECIFIED,
                    itemId = "",
                    nameSnapshot = "Домашний суп",
                    weightGrams = 300.0,
                    nutrition = NutritionFacts(calories = 180.0, proteins = 12.0, fats = 5.0, carbs = 20.0),
                ),
            ),
        )
        val recordedAt = Instant.fromEpochMilliseconds(1700005000000L)

        val result = useCase(day = day, recordedAt = recordedAt)

        assertTrue(result.isSuccess)
        val records = result.getOrThrow()
        assertEquals(3, records.size)
        assertEquals(3, diaryRepository.addEntryCallCount)

        assertEquals(EntrySourceType.RECIPE, records[0].sourceType)
        assertEquals("rec-oatmeal", records[0].sourceId)
        assertEquals(MealType.BREAKFAST, records[0].mealType)
        assertEquals(Money.fromMinor(4500L), records[0].cost)
        assertEquals(SyncStatus.PENDING, records[0].syncStatus)

        assertEquals(EntrySourceType.PRODUCT, records[1].sourceType)
        assertEquals("prod-apple", records[1].sourceId)
        assertEquals(MealType.SNACK, records[1].mealType)
        assertNull(records[1].cost)

        assertEquals(EntrySourceType.CUSTOM, records[2].sourceType)
        assertNull(records[2].sourceId)
        assertEquals(MealType.DINNER, records[2].mealType)

        assertNotEquals(records[0].id, records[1].id)
        assertNotEquals(records[0].idempotencyKey, records[1].idempotencyKey)
    }

    @Test
    fun `invoke with MealPlanTemplate applies requested dayNumber and fails with NotFound if missing`() = runTest {
        val planId = MealPlanTemplateId("plan-coach")
        val day2 = MealPlanDay(
            dayNumber = 2,
            items = listOf(
                MealPlanItem(
                    id = MealPlanItemId("item-d2"),
                    planId = planId,
                    dayNumber = 2,
                    mealSlot = MealType.LUNCH,
                    itemType = PlanItemType.RECIPE,
                    itemId = "rec-salad",
                    nameSnapshot = "Салат с тунцом",
                    weightGrams = 220.0,
                    nutrition = NutritionFacts(calories = 260.0, proteins = 24.0, fats = 12.0, carbs = 14.0),
                ),
            ),
        )
        val template = MealPlanTemplate(
            id = planId,
            authorId = UserId("coach-1"),
            title = "Неделя сушки",
            durationDays = 2,
            days = listOf(day2),
        )

        val success = useCase(plan = template, dayNumber = 2)
        assertTrue(success.isSuccess)
        assertEquals(1, success.getOrThrow().size)
        assertEquals("Салат с тунцом", success.getOrThrow().first().nameSnapshot)

        val missing = useCase(plan = template, dayNumber = 99)
        assertTrue(missing.isFailure)
        assertIs<DomainError.NotFound>(missing.exceptionOrNull())
    }

    @Test
    fun `invoke returns ValidationError when day has no items or invalid item weight`() = runTest {
        val emptyDayResult = useCase(MealPlanDay(dayNumber = 1, items = emptyList()))
        assertTrue(emptyDayResult.isFailure)
        val emptyErr = assertIs<DomainError.ValidationError>(emptyDayResult.exceptionOrNull())
        assertEquals("items", emptyErr.field)

        val invalidWeightDay = MealPlanDay(
            dayNumber = 1,
            items = listOf(
                MealPlanItem(
                    id = MealPlanItemId("bad-item"),
                    planId = MealPlanTemplateId("plan-1"),
                    dayNumber = 1,
                    mealSlot = MealType.BREAKFAST,
                    itemType = PlanItemType.PRODUCT,
                    itemId = "prod-1",
                    nameSnapshot = "Банан",
                    weightGrams = 0.0,
                    nutrition = NutritionFacts.ZERO,
                ),
            ),
        )
        val badWeightResult = useCase(invalidWeightDay)
        assertTrue(badWeightResult.isFailure)
        val weightErr = assertIs<DomainError.ValidationError>(badWeightResult.exceptionOrNull())
        assertEquals("weightGrams", weightErr.field)
        assertEquals(0, diaryRepository.addEntryCallCount)
    }

    @Test
    fun `invoke stops and returns failure when repository fails mid-batch`() = runTest {
        val planId = MealPlanTemplateId("plan-fail")
        val day = MealPlanDay(
            dayNumber = 1,
            items = listOf(
                MealPlanItem(
                    id = MealPlanItemId("i1"),
                    planId = planId,
                    dayNumber = 1,
                    mealSlot = MealType.BREAKFAST,
                    itemType = PlanItemType.RECIPE,
                    itemId = "r1",
                    nameSnapshot = "Блюдо 1",
                    weightGrams = 100.0,
                    nutrition = NutritionFacts(calories = 200.0),
                ),
                MealPlanItem(
                    id = MealPlanItemId("i2"),
                    planId = planId,
                    dayNumber = 1,
                    mealSlot = MealType.LUNCH,
                    itemType = PlanItemType.RECIPE,
                    itemId = "r2",
                    nameSnapshot = "Блюдо 2",
                    weightGrams = 200.0,
                    nutrition = NutritionFacts(calories = 400.0),
                ),
            ),
        )
        diaryRepository.shouldFailAddEntry = DomainError.Unknown("Disk full")
        diaryRepository.failOnAddCallNumber = 2

        val result = useCase(day)

        assertTrue(result.isFailure)
        assertIs<DomainError.Unknown>(result.exceptionOrNull())
        assertEquals(2, diaryRepository.addEntryCallCount)
    }
}
