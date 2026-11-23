package dev.andre.nutritionist.presentation.addmeal

import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItem
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class AddMealUpdateTest {

    private val update = AddMealUpdate()
    private val testUserId = UserId("user-100")
    private val testDate = LocalDate(2026, 3, 15)

    private val sampleFood = FoodItem(
        id = FoodItemId("food-chicken"),
        name = "Куриное филе",
        brand = "Петелинка",
        caloriesPer100g = 165,
        proteinsPer100g = 31.0,
        fatsPer100g = 3.6,
        carbsPer100g = 0.0,
        defaultPortionGrams = 150.0,
        category = "Обед",
    )

    @Test
    fun `Init sets Loading state and emits SearchFoods command`() {
        val initial = AddMealState()
        val next = update.update(initial, AddMealEvent.Ui.Init(MealType.LUNCH, testUserId, testDate))

        val state = requireNotNull(next.state)
        assertEquals(MealType.LUNCH, state.mealType)
        assertEquals(testUserId, state.userId)
        assertEquals(testDate, state.date)
        assertIs<AddMealState.Content.Loading>(state.content)
        assertEquals(listOf(AddMealCommand.SearchFoods("", "Все")), next.commands)
    }

    @Test
    fun `SearchQueryChanged updates query and emits SearchFoods command`() {
        val initial = AddMealState(selectedCategory = "Обед")
        val next = update.update(initial, AddMealEvent.Ui.SearchQueryChanged("филе"))

        val state = requireNotNull(next.state)
        assertEquals("филе", state.searchQuery)
        assertIs<AddMealState.Content.Loading>(state.content)
        assertEquals(listOf(AddMealCommand.SearchFoods("филе", "Обед")), next.commands)
    }

    @Test
    fun `CategorySelected updates category and emits SearchFoods command`() {
        val initial = AddMealState(searchQuery = "гречка")
        val next = update.update(initial, AddMealEvent.Ui.CategorySelected("Обед"))

        val state = requireNotNull(next.state)
        assertEquals("Обед", state.selectedCategory)
        assertIs<AddMealState.Content.Loading>(state.content)
        assertEquals(listOf(AddMealCommand.SearchFoods("гречка", "Обед")), next.commands)
    }

    @Test
    fun `FoodSelected updates selectedFoodItem and portion`() {
        val initial = AddMealState()
        val next = update.update(initial, AddMealEvent.Ui.FoodSelected(sampleFood))

        val state = requireNotNull(next.state)
        assertEquals(sampleFood, state.selectedFoodItem)
        assertEquals(150.0, state.portionGrams)
    }

    @Test
    fun `PortionChanged updates portion clamped to minimum 1`() {
        val initial = AddMealState(portionGrams = 100.0)
        val next = update.update(initial, AddMealEvent.Ui.PortionChanged(200.0))
        val state = requireNotNull(next.state)
        assertEquals(200.0, state.portionGrams)

        val nextZero = update.update(initial, AddMealEvent.Ui.PortionChanged(0.0))
        val stateZero = requireNotNull(nextZero.state)
        assertEquals(1.0, stateZero.portionGrams)
    }

    @Test
    fun `ConfirmAdd with selected food emits SaveDiaryEntry command with calculated macros`() {
        val initial = AddMealState(
            mealType = MealType.LUNCH,
            userId = testUserId,
            date = testDate,
            selectedFoodItem = sampleFood,
            portionGrams = 200.0, // 2x factor
        )

        val next = update.update(initial, AddMealEvent.Ui.ConfirmAdd)
        assertEquals(1, next.commands.size)
        val command = next.commands.first()
        assertIs<AddMealCommand.SaveDiaryEntry>(command)
        val entry = command.entry

        assertEquals(testUserId, entry.userId)
        assertEquals(sampleFood.id, entry.foodItemId)
        assertEquals(MealType.LUNCH, entry.mealType)
        assertEquals(330, entry.calories) // 165 * 2
        assertEquals(62.0, entry.proteins) // 31 * 2
        assertEquals(7.2, entry.fats) // 3.6 * 2
        assertEquals(0.0, entry.carbs)
    }

    @Test
    fun `ConfirmAdd without selected food produces ShowSnackbar news`() {
        val initial = AddMealState(userId = testUserId, date = testDate, selectedFoodItem = null)
        val next = update.update(initial, AddMealEvent.Ui.ConfirmAdd)

        assertTrue(next.commands.isEmpty())
        assertEquals(1, next.news.size)
        assertIs<AddMealNews.ShowSnackbar>(next.news.first())
    }

    @Test
    fun `Dismiss, BarcodeScanClick and AiCameraClick emit corresponding news`() {
        val initial = AddMealState()

        val nextDismiss = update.update(initial, AddMealEvent.Ui.Dismiss)
        assertEquals(listOf(AddMealNews.Dismiss), nextDismiss.news)

        val nextBarcode = update.update(initial, AddMealEvent.Ui.BarcodeScanClick)
        assertEquals(listOf(AddMealNews.OpenBarcodeScanner), nextBarcode.news)

        val nextCamera = update.update(initial, AddMealEvent.Ui.AiCameraClick)
        assertEquals(listOf(AddMealNews.OpenAiCamera), nextCamera.news)
    }

    @Test
    fun `Internal FoodsLoaded updates state content to Success`() {
        val initial = AddMealState(content = AddMealState.Content.Loading)
        val next = update.update(initial, AddMealEvent.Internal.FoodsLoaded(listOf(sampleFood)))

        val state = requireNotNull(next.state)
        val content = state.content
        assertIs<AddMealState.Content.Success>(content)
        assertEquals(listOf(sampleFood), content.foods)
    }

    @Test
    fun `NetworkStatusChanged updates isOnline in state`() {
        val initial = AddMealState(isOnline = true)
        val next = update.update(initial, AddMealEvent.Ui.NetworkStatusChanged(false))

        val state = requireNotNull(next.state)
        assertEquals(false, state.isOnline)
    }

    @Test
    fun `Internal EntrySaved emits MealAddedSuccess, online ShowSnackbar and Dismiss when online`() {
        val initial = AddMealState(isOnline = true)
        val entry = DiaryEntry(
            id = DiaryEntryId("e-1"),
            userId = testUserId,
            foodItemId = sampleFood.id,
            foodName = "Курица",
            portionGrams = 100.0,
            calories = 165,
            proteins = 31.0,
            fats = 3.6,
            carbs = 0.0,
            mealType = MealType.LUNCH,
            date = testDate,
            recordedAt = Instant.fromEpochMilliseconds(0),
            idempotencyKey = IdempotencyKey("idem-1"),
        )

        val next = update.update(initial, AddMealEvent.Internal.EntrySaved(entry))
        assertEquals(
            listOf(
                AddMealNews.MealAddedSuccess(entry),
                AddMealNews.ShowSnackbar("Блюдо добавлено в дневник"),
                AddMealNews.Dismiss,
            ),
            next.news,
        )
    }

    @Test
    fun `Internal EntrySaved emits MealAddedSuccess, offline ShowSnackbar and Dismiss when offline`() {
        val initial = AddMealState(isOnline = false)
        val entry = DiaryEntry(
            id = DiaryEntryId("e-1"),
            userId = testUserId,
            foodItemId = sampleFood.id,
            foodName = "Курица",
            portionGrams = 100.0,
            calories = 165,
            proteins = 31.0,
            fats = 3.6,
            carbs = 0.0,
            mealType = MealType.LUNCH,
            date = testDate,
            recordedAt = Instant.fromEpochMilliseconds(0),
            idempotencyKey = IdempotencyKey("idem-1"),
        )

        val next = update.update(initial, AddMealEvent.Internal.EntrySaved(entry))
        assertEquals(
            listOf(
                AddMealNews.MealAddedSuccess(entry),
                AddMealNews.ShowSnackbar("Сохранено локально. Отправится при появлении сети"),
                AddMealNews.Dismiss,
            ),
            next.news,
        )
    }
}
