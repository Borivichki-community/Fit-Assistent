package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.catalog.v1.AveragePriceItemProto
import com.budgetntr.proto.catalog.v1.CatalogNutritionFactsProto
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.GetProductResponse
import com.budgetntr.proto.catalog.v1.ProductItemProto
import com.budgetntr.proto.catalog.v1.RegionalPriceItemProto
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.CostSnapshotProto
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DeleteMealResponse
import com.budgetntr.proto.diary.v1.DiaryEntryItemProto
import com.budgetntr.proto.diary.v1.EntrySourceTypeProto
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import com.budgetntr.proto.diary.v1.MealNutritionSummaryProto
import com.budgetntr.proto.diary.v1.MoneyProto
import com.budgetntr.proto.diary.v1.NutritionSummaryProto
import com.budgetntr.proto.profile.v1.TimestampProto
import com.budgetntr.proto.recipe.v1.GenerateMealPlanRequest
import com.budgetntr.proto.recipe.v1.GenerateMealPlanResponse
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuRequest
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuResponse
import com.budgetntr.proto.recipe.v1.GeneratedMealProto
import com.budgetntr.proto.recipe.v1.MealTypeProto
import com.budgetntr.proto.recipe.v1.MenuIngredientProto
import com.budgetntr.proto.recipe.v1.MenuMealProto
import com.budgetntr.proto.recipe.v1.MenuSourceProto
import com.budgetntr.proto.recipe.v1.TargetNutritionProto
import com.budgetntr.proto.recipe.v1.TargetPeriodProto
import com.budgetntr.proto.recipe.v1.WeekMenuDayProto
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidGrpcClientsSerializationTest {

    @Test
    fun catalog_searchProducts_roundtripSerialization() {
        val request = SearchProductsRequest(
            query = "Курица",
            category = "Мясо и птица",
            minCalories = 100.0,
            maxCalories = 250.0,
            minProteins = 20.0,
            pageSize = 10,
            pageToken = "page-1",
        )
        val reqStream = SearchProductsRequestMarshaller.stream(request)
        val deserializedReq = SearchProductsRequestMarshaller.parse(reqStream)
        assertEquals(request, deserializedReq)

        val response = SearchProductsResponse(
            products = listOf(
                ProductItemProto(
                    id = "prod-1",
                    name = "Куриное филе",
                    barcode = "460123456789",
                    category = "Мясо",
                    nutrition = CatalogNutritionFactsProto(113.0, 23.6, 1.9, 0.0),
                    version = 1L,
                    prices = listOf(
                        RegionalPriceItemProto(
                            id = "p-1",
                            productId = "prod-1",
                            regionId = "reg-msk",
                            price = 380.0,
                            currency = "RUB",
                            unit = "кг",
                            regionName = "Москва",
                        )
                    )
                )
            ),
            nextPageToken = "next-tok",
        )
        val respStream = SearchProductsResponseMarshaller.stream(response)
        val deserializedResp = SearchProductsResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }

    @Test
    fun catalog_getProduct_roundtripSerialization() {
        val request = GetProductRequest(productId = "prod-42", regionId = "reg-spb")
        val stream = GetProductRequestMarshaller.stream(request)
        val deserialized = GetProductRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val response = GetProductResponse(
            product = ProductItemProto(id = "prod-42", name = "Гречка", category = "Крупы")
        )
        val respStream = GetProductResponseMarshaller.stream(response)
        val deserializedResp = GetProductResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }

    @Test
    fun catalog_getAveragePrices_roundtripSerialization() {
        val request = GetAveragePricesRequest(productIds = listOf("p1", "p2"), regionId = "reg-1")
        val stream = GetAveragePricesRequestMarshaller.stream(request)
        val deserialized = GetAveragePricesRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val response = GetAveragePricesResponse(
            prices = listOf(
                AveragePriceItemProto("p1", 120.0, "RUB", 5),
                AveragePriceItemProto("p2", 340.5, "RUB", 3),
            )
        )
        val respStream = GetAveragePricesResponseMarshaller.stream(response)
        val deserializedResp = GetAveragePricesResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }

    @Test
    fun diary_addEntry_roundtripSerialization() {
        val request = AddEntryRequest(
            entryId = "entry-001",
            mealType = "BREAKFAST",
            recordedAt = TimestampProto(1700000000L, 0),
            sourceType = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT,
            sourceId = "prod-1",
            nameSnapshot = "Овсяная каша",
            weightGrams = 150.0,
            nutrition = NutritionSummaryProto(calories = 250, proteins = 8.5, fats = 4.2, carbs = 42.0),
            cost = CostSnapshotProto(total = MoneyProto(amountMinor = 4500L, currencyCode = "RUB")),
        )
        val stream = AddEntryRequestMarshaller.stream(request)
        val deserialized = AddEntryRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val response = AddEntryResponse(
            entry = DiaryEntryItemProto(
                id = "entry-001",
                mealType = "BREAKFAST",
                sourceType = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT,
                nameSnapshot = "Овсяная каша",
                weightGrams = 150.0,
                nutrition = NutritionSummaryProto(250, 8.5, 4.2, 42.0),
                recordedAt = TimestampProto(1700000000L, 0),
                cost = CostSnapshotProto(total = MoneyProto(4500L, "RUB")),
            )
        )
        val respStream = AddEntryResponseMarshaller.stream(response)
        val deserializedResp = AddEntryResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }

    @Test
    fun diary_getDailyEntries_roundtripSerialization() {
        val request = GetDailyEntriesRequest(date = "2026-11-24", timeZone = "Europe/Moscow")
        val stream = GetDailyEntriesRequestMarshaller.stream(request)
        val deserialized = GetDailyEntriesRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val response = GetDailyEntriesResponse(
            entries = listOf(
                DiaryEntryItemProto(
                    id = "e1",
                    mealType = "LUNCH",
                    nameSnapshot = "Борщ",
                    weightGrams = 300.0,
                    nutrition = NutritionSummaryProto(220, 10.0, 6.0, 25.0),
                )
            ),
            dailyTotal = NutritionSummaryProto(220, 10.0, 6.0, 25.0),
            mealTotals = listOf(
                MealNutritionSummaryProto(
                    mealType = "LUNCH",
                    nutrition = NutritionSummaryProto(220, 10.0, 6.0, 25.0),
                )
            ),
        )
        val respStream = GetDailyEntriesResponseMarshaller.stream(response)
        val deserializedResp = GetDailyEntriesResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }

    @Test
    fun diary_deleteMeal_roundtripSerialization() {
        val request = DeleteMealRequest(entryId = "entry-to-del")
        val stream = DeleteMealRequestMarshaller.stream(request)
        val deserialized = DeleteMealRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val respStream = DeleteMealResponseMarshaller.stream(DeleteMealResponse())
        val deserializedResp = DeleteMealResponseMarshaller.parse(respStream)
        assertEquals(true, deserializedResp.success)
    }

    @Test
    fun recipe_generateMealPlan_roundtripSerialization() {
        val request = GenerateMealPlanRequest(
            period = TargetPeriodProto.TARGET_PERIOD_DINNER,
            targetNutrition = TargetNutritionProto(650.0, 60.0, 10.0, 70.0),
            maxBudgetMinor = 30000L,
            currency = "RUB",
            regionId = "reg-msk",
            preferredProducts = listOf("prod-chicken", "prod-rice"),
            userNote = "Поменьше масла",
        )
        val stream = GenerateMealPlanRequestMarshaller.stream(request)
        val deserialized = GenerateMealPlanRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val response = GenerateMealPlanResponse(
            planId = "plan-007",
            period = TargetPeriodProto.TARGET_PERIOD_DINNER,
            meals = listOf(
                GeneratedMealProto(
                    type = MealTypeProto.MEAL_TYPE_DINNER,
                    title = "Куриное филе на пару",
                    ingredients = listOf(
                        MenuIngredientProto("prod-chicken", "Куриное филе", 200),
                        MenuIngredientProto("prod-rice", "Рис", 80),
                    ),
                    nutrition = TargetNutritionProto(626.5, 66.0, 8.5, 70.0),
                    estimatedCostMinor = 10500L,
                    cookingTimeMinutes = 25,
                )
            ),
            totalNutrition = TargetNutritionProto(626.5, 66.0, 8.5, 70.0),
            estimatedCostMinor = 10500L,
            currency = "RUB",
            cookingInstructions = "Отварить рис и приготовить филе",
            source = MenuSourceProto.MENU_SOURCE_GENERATED,
        )
        val respStream = GenerateMealPlanResponseMarshaller.stream(response)
        val deserializedResp = GenerateMealPlanResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }

    @Test
    fun recipe_generateWeekMenu_roundtripSerialization() {
        val request = GenerateWeekMenuRequest(
            dailyCalories = 2200,
            weeklyBudgetMinor = 450000L,
            excludedProductIds = listOf("p-allergy"),
            preferredProductIds = listOf("p-fav"),
        )
        val stream = GenerateWeekMenuRequestMarshaller.stream(request)
        val deserialized = GenerateWeekMenuRequestMarshaller.parse(stream)
        assertEquals(request, deserialized)

        val response = GenerateWeekMenuResponse(
            dayReady = WeekMenuDayProto(
                dayNumber = 1,
                meals = listOf(
                    MenuMealProto(
                        type = MealTypeProto.MEAL_TYPE_BREAKFAST,
                        name = "Овсяная каша с ягодами",
                        ingredients = listOf(MenuIngredientProto("oats", "Овсянка", 100)),
                        calories = 350.0,
                        estimatedCostMinor = 5500L,
                    )
                ),
                calories = 2150.0,
                estimatedCostMinor = 65000L,
            )
        )
        val respStream = GenerateWeekMenuResponseMarshaller.stream(response)
        val deserializedResp = GenerateWeekMenuResponseMarshaller.parse(respStream)
        assertEquals(response, deserializedResp)
    }
}
