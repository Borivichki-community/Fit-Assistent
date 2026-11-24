package dev.andre.nutritionist.recipe

import com.budgetntr.proto.catalog.v1.AveragePriceItem
import com.budgetntr.proto.catalog.v1.CatalogServiceGrpcKt
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.NutritionFacts
import com.budgetntr.proto.catalog.v1.ProductItem
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.CostSnapshot
import com.budgetntr.proto.diary.v1.DiaryEntryItem
import com.budgetntr.proto.diary.v1.DiaryServiceGrpcKt
import com.budgetntr.proto.diary.v1.EntrySourceType
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import com.budgetntr.proto.diary.v1.MealNutritionSummary
import com.budgetntr.proto.diary.v1.Money
import com.budgetntr.proto.diary.v1.NutritionSummary
import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.ProfileServiceGrpcKt
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import com.budgetntr.proto.recipe.v1.GenerateMealPlanRequest
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import com.budgetntr.proto.recipe.v1.TargetNutrition
import com.budgetntr.proto.recipe.v1.TargetPeriod
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.recipe.config.RecipeConfiguration
import dev.andre.nutritionist.recipe.config.RecipeProperties
import dev.andre.nutritionist.recipe.domain.service.RecipeMenuService
import dev.andre.nutritionist.recipe.domain.service.ResilientMenuGenerator
import dev.andre.nutritionist.recipe.domain.validator.MenuValidator
import dev.andre.nutritionist.recipe.infrastructure.grpc.RecipeGrpcService
import dev.andre.nutritionist.recipe.infrastructure.grpc.RecipeIdentityInterceptor
import dev.andre.nutritionist.recipe.infrastructure.grpc.client.CatalogClient
import dev.andre.nutritionist.recipe.infrastructure.llm.GeminiLlmClient
import dev.andre.nutritionist.recipe.infrastructure.llm.prompt.MealPlanPromptBuilder
import dev.andre.nutritionist.recipe.infrastructure.redis.RedisMenuStore
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant
import java.util.UUID

class E2EFourServiceChainTest {
    @RegisterExtension
    @JvmField
    val wire = WireMockExtension.newInstance().options(options().dynamicPort()).build()

    private val json = JsonMapper.builder().build()
    private val meters = SimpleMeterRegistry()
    private val configuration = RecipeConfiguration()

    // ── 1. In-process Mock Profile Service ────────────────────────────────────
    class InProcessProfileService : ProfileServiceGrpcKt.ProfileServiceCoroutineImplBase() {
        private val profiles = mutableMapOf<String, ProfileProto>()

        override suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto {
            val proto =
                ProfileProto.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setUserId(request.userId)
                    .setHeightCm(request.heightCm)
                    .setWeightKg(request.weightKg)
                    .setGender(request.gender)
                    .setActivityLevel(request.activityLevel)
                    .setGoalType(request.goalType)
                    .setNutritionGoal(request.nutritionGoal)
                    .setWeeklyBudget(request.weeklyBudget)
                    .setUpdatedAt(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                    .build()
            profiles[request.userId] = proto
            return proto
        }
    }

    // ── 2. In-process Mock Catalog Service ────────────────────────────────────
    class InProcessCatalogService : CatalogServiceGrpcKt.CatalogServiceCoroutineImplBase() {
        override suspend fun searchProducts(request: SearchProductsRequest): SearchProductsResponse {
            val chicken =
                ProductItem.newBuilder()
                    .setId("20000000-0000-0000-0000-000000000057")
                    .setName("Куриное филе грудки охл")
                    .setNutrition(
                        NutritionFacts.newBuilder()
                            .setCaloriesPer100G(113.0)
                            .setProteinsPer100G(23.6)
                            .setFatsPer100G(1.9)
                            .setCarbsPer100G(0.0)
                            .build(),
                    ).build()

            val rice =
                ProductItem.newBuilder()
                    .setId("20000000-0000-0000-0000-000000000003")
                    .setName("Рис круглозерный шлифованный")
                    .setNutrition(
                        NutritionFacts.newBuilder()
                            .setCaloriesPer100G(344.0)
                            .setProteinsPer100G(7.0)
                            .setFatsPer100G(1.0)
                            .setCarbsPer100G(74.0)
                            .build(),
                    ).build()

            return SearchProductsResponse.newBuilder()
                .addAllProducts(listOf(chicken, rice))
                .build()
        }

        override suspend fun getAveragePrices(request: GetAveragePricesRequest): GetAveragePricesResponse =
            GetAveragePricesResponse.newBuilder()
                .addAllPrices(
                    listOf(
                        AveragePriceItem.newBuilder()
                            .setProductId("20000000-0000-0000-0000-000000000057")
                            .setAveragePrice(35.0) // 35 RUB per 100g
                            .setCurrency("RUB")
                            .setRegionCount(1)
                            .build(),
                        AveragePriceItem.newBuilder()
                            .setProductId("20000000-0000-0000-0000-000000000003")
                            .setAveragePrice(15.0) // 15 RUB per 100g
                            .setCurrency("RUB")
                            .setRegionCount(1)
                            .build(),
                    ),
                ).build()
    }

    // ── 4. In-process Mock Diary Service ──────────────────────────────────────
    class InProcessDiaryService : DiaryServiceGrpcKt.DiaryServiceCoroutineImplBase() {
        private val entries = mutableListOf<DiaryEntryItem>()

        override suspend fun addEntry(request: AddEntryRequest): AddEntryResponse {
            val item =
                DiaryEntryItem.newBuilder()
                    .setId(request.entryId.ifBlank { UUID.randomUUID().toString() })
                    .setSourceType(request.sourceType)
                    .apply { if (request.hasSourceId()) setSourceId(request.sourceId) }
                    .setNameSnapshot(request.nameSnapshot)
                    .setWeightGrams(request.weightGrams)
                    .setNutrition(request.nutrition)
                    .setMealType(request.mealType)
                    .setCost(request.cost)
                    .setRecordedAt(request.recordedAt)
                    .build()
            entries.add(item)
            return AddEntryResponse.newBuilder().setEntry(item).build()
        }

        override suspend fun getDailyEntries(request: GetDailyEntriesRequest): GetDailyEntriesResponse {
            val totalCalories = entries.sumOf { it.nutrition.calories }
            val totalProteins = entries.sumOf { it.nutrition.proteins }
            val totalFats = entries.sumOf { it.nutrition.fats }
            val totalCarbs = entries.sumOf { it.nutrition.carbs }

            val dailyTotal =
                NutritionSummary.newBuilder()
                    .setCalories(totalCalories)
                    .setProteins(totalProteins)
                    .setFats(totalFats)
                    .setCarbs(totalCarbs)
                    .build()

            val mealTotals =
                entries.groupBy { it.mealType }.map { (type, list) ->
                    MealNutritionSummary.newBuilder()
                        .setMealType(type)
                        .setNutrition(
                            NutritionSummary.newBuilder()
                                .setCalories(list.sumOf { it.nutrition.calories })
                                .setProteins(list.sumOf { it.nutrition.proteins })
                                .setFats(list.sumOf { it.nutrition.fats })
                                .setCarbs(list.sumOf { it.nutrition.carbs })
                                .build(),
                        ).build()
                }

            return GetDailyEntriesResponse.newBuilder()
                .addAllEntries(entries)
                .setDailyTotal(dailyTotal)
                .addAllMealTotals(mealTotals)
                .build()
        }
    }

    @Test
    fun `complete end-to-end 4-step chain from profile to diary`() =
        runBlocking {
            // Setup Profile Server
            val profileName = InProcessServerBuilder.generateName()
            val profileServer = InProcessServerBuilder.forName(profileName).directExecutor().addService(InProcessProfileService()).build().start()
            val profileChannel = InProcessChannelBuilder.forName(profileName).directExecutor().build()
            val profileStub = ProfileServiceGrpcKt.ProfileServiceCoroutineStub(profileChannel)

            // Setup Catalog Server
            val catalogName = InProcessServerBuilder.generateName()
            val catalogServer = InProcessServerBuilder.forName(catalogName).directExecutor().addService(InProcessCatalogService()).build().start()
            val catalogChannel = InProcessChannelBuilder.forName(catalogName).directExecutor().build()
            val catalogStub = CatalogServiceGrpcKt.CatalogServiceCoroutineStub(catalogChannel)

            // Setup Diary Server
            val diaryName = InProcessServerBuilder.generateName()
            val diaryServer = InProcessServerBuilder.forName(diaryName).directExecutor().addService(InProcessDiaryService()).build().start()
            val diaryChannel = InProcessChannelBuilder.forName(diaryName).directExecutor().build()
            val diaryStub = DiaryServiceGrpcKt.DiaryServiceCoroutineStub(diaryChannel)

            // Setup Recipe Service with mocked Catalog Client
            val settings =
                RecipeProperties(
                    llmApiKey = "test-gemini-key",
                    llmBaseUrl = wire.baseUrl(),
                    llmTimeout = Duration.ofSeconds(5),
                    llmRetryDelay = Duration.ofMillis(10),
                )
            val catalogClient = CatalogClient(catalogChannel, settings)
            val promptBuilder = MealPlanPromptBuilder(json)
            val validator = MenuValidator(json, settings)
            val redis = mock(RedisMenuStore::class.java)

            val recipeServerName = InProcessServerBuilder.generateName()
            var recipeServer: io.grpc.Server?
            var recipeChannel: io.grpc.ManagedChannel?

            configuration.llmHttpClient().use { http ->
                val llmClient = GeminiLlmClient(http, json, settings, meters)
                val generator =
                    ResilientMenuGenerator(
                        llmClient,
                        validator,
                        promptBuilder,
                        configuration.recipeCircuitBreakers(settings, meters),
                        configuration.recipeRetries(settings),
                        configuration.recipeTimeLimiter(settings),
                    )
                val recipeService =
                    RecipeMenuService(
                        catalogClient,
                        generator,
                        validator,
                        store = redis,
                        settings = settings,
                        json = json,
                        meters = meters,
                    )
                val rpc = RecipeGrpcService(recipeService)

                val interceptor =
                    object : ServerInterceptor {
                        override fun <ReqT, RespT> interceptCall(
                            call: ServerCall<ReqT, RespT>,
                            headers: Metadata,
                            next: ServerCallHandler<ReqT, RespT>,
                        ): ServerCall.Listener<ReqT> {
                            val context =
                                Context.current().withValue(
                                    RecipeIdentityInterceptor.requestContext,
                                    dev.andre.nutritionist.recipe.domain.model.RequestContext(
                                        dev.andre.nutritionist.recipe.domain.model.UserId("usr_test_flow"),
                                    ),
                                )
                            return Contexts.interceptCall(context, call, headers, next)
                        }
                    }

                recipeServer =
                    InProcessServerBuilder.forName(recipeServerName)
                        .directExecutor()
                        .addService(ServerInterceptors.intercept(rpc, interceptor))
                        .build()
                        .start()

                recipeChannel = InProcessChannelBuilder.forName(recipeServerName).directExecutor().build()
                val recipeStub = RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineStub(recipeChannel)

                try {
                    val userId = "usr_test_flow"

                    // =========================================================================
                    // ШАГ 1: UpsertProfile (UpdateProfile)
                    // =========================================================================
                    println("=== ШАГ 1: UpsertProfile ===")
                    val profileRequest =
                        UpdateProfileRequest.newBuilder()
                            .setUserId(userId)
                            .setHeightCm(180)
                            .setWeightKg(75.0)
                            .setGender(GenderProto.GENDER_MALE)
                            .setActivityLevel(ActivityLevelProto.ACTIVITY_LEVEL_MODERATE)
                            .setGoalType(GoalTypeProto.GOAL_TYPE_MAINTAIN)
                            .setNutritionGoal(
                                NutritionGoalProto.newBuilder()
                                    .setTargetCalories(2200)
                                    .setTargetProteinsGrams(150.0)
                                    .setTargetFatsGrams(70.0)
                                    .setTargetCarbsGrams(240.0)
                                    .setTargetWeightKg(75.0)
                                    .build(),
                            ).setWeeklyBudget(4500.0)
                            .build()

                    val profileResponse = profileStub.updateProfile(profileRequest)
                    assertNotNull(profileResponse.id)
                    assertEquals(userId, profileResponse.userId)
                    assertEquals(2200, profileResponse.nutritionGoal.targetCalories)
                    assertEquals(4500.0, profileResponse.weeklyBudget)
                    println("Профиль успешно создан: ID=${profileResponse.id}, целевые ккал=${profileResponse.nutritionGoal.targetCalories}, бюджет=${profileResponse.weeklyBudget} RUB")

                    // =========================================================================
                    // ШАГ 2: SearchProducts
                    // =========================================================================
                    println("\n=== ШАГ 2: SearchProducts ===")
                    val searchRequest =
                        SearchProductsRequest.newBuilder()
                            .setQuery("курица")
                            .setPageSize(10)
                            .build()

                    val catalogResponse = catalogStub.searchProducts(searchRequest)
                    assertTrue(catalogResponse.productsCount > 0)
                    val chickenProduct = catalogResponse.productsList.first { it.name.contains("Куриное") }
                    assertEquals("20000000-0000-0000-0000-000000000057", chickenProduct.id)
                    assertEquals(113.0, chickenProduct.nutrition.caloriesPer100G)
                    println("Продукт найден: ID=${chickenProduct.id}, Название='${chickenProduct.name}', Ккал/100г=${chickenProduct.nutrition.caloriesPer100G}")

                    // =========================================================================
                    // ШАГ 3: GenerateMealPlan
                    // =========================================================================
                    println("\n=== ШАГ 3: GenerateMealPlan ===")
                    val mockLlmResponse =
                        """
                        {
                          "meals": [
                            {
                              "type": "dinner",
                              "title": "Куриное филе на пару с отварным рисом",
                              "products": [
                                { "id": "20000000-0000-0000-0000-000000000057", "grams": 250 },
                                { "id": "20000000-0000-0000-0000-000000000003", "grams": 100 }
                              ]
                            }
                          ],
                          "cookingInstructions": "Отварите рис 15 минут в подсоленной воде. Куриное филе приготовьте на пару в течение 20 минут."
                        }
                        """.trimIndent()

                    wire.stubFor(
                        post("/v1beta/models/gemini-2.5-flash:generateContent")
                            .willReturn(okJson(envelope(mockLlmResponse))),
                    )

                    val mealPlanRequest =
                        GenerateMealPlanRequest.newBuilder()
                            .setPeriod(TargetPeriod.TARGET_PERIOD_DINNER)
                            .setTargetNutrition(
                                TargetNutrition.newBuilder()
                                    .setCalories(626.0)
                                    .setProteinsGrams(66.0)
                                    .setFatsGrams(5.7)
                                    .setCarbsGrams(74.0)
                                    .build(),
                            ).setMaxBudgetMinor(20000) // 200 RUB
                            .addPreferredProducts("Куриное филе грудки охл")
                            .setUserNote("Высокобелковый ужин после тренировки")
                            .build()

                    val mealPlanResponse =
                        recipeStub
                            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(Metadata().apply { put(CatalogClient.devBypassHeader, "true") }))
                            .generateMealPlan(mealPlanRequest)

                    assertNotNull(mealPlanResponse.planId)
                    assertEquals(TargetPeriod.TARGET_PERIOD_DINNER, mealPlanResponse.period)
                    assertEquals(1, mealPlanResponse.mealsCount)
                    val generatedDinner = mealPlanResponse.mealsList.first()
                    assertEquals("Куриное филе на пару с отварным рисом", generatedDinner.title)
                    assertEquals(2, generatedDinner.ingredientsCount)
                    println("План питания сгенерирован: ID=${mealPlanResponse.planId}")
                    println("Блюдо: '${generatedDinner.title}', Ккал=${generatedDinner.nutrition.calories}, Стоимость=${generatedDinner.estimatedCostMinor} копеек")
                    println("Инструкция: ${mealPlanResponse.cookingInstructions}")

                    // =========================================================================
                    // ШАГ 4: AddEntry + GetDailyEntries в Diary
                    // =========================================================================
                    println("\n=== ШАГ 4: AddEntry + GetDailyEntries в Diary ===")
                    val addEntryReq =
                        AddEntryRequest.newBuilder()
                            .setEntryId(UUID.randomUUID().toString())
                            .setMealType("dinner")
                            .setSourceType(EntrySourceType.ENTRY_SOURCE_TYPE_RECIPE)
                            .setSourceId(mealPlanResponse.planId)
                            .setNameSnapshot(generatedDinner.title)
                            .setWeightGrams(350.0)
                            .setNutrition(
                                NutritionSummary.newBuilder()
                                    .setCalories(generatedDinner.nutrition.calories.toInt())
                                    .setProteins(generatedDinner.nutrition.proteinsGrams)
                                    .setFats(generatedDinner.nutrition.fatsGrams)
                                    .setCarbs(generatedDinner.nutrition.carbsGrams)
                                    .build(),
                            ).setCost(
                                CostSnapshot.newBuilder()
                                    .setTotal(
                                        Money.newBuilder()
                                            .setAmountMinor(generatedDinner.estimatedCostMinor)
                                            .setCurrencyCode("RUB")
                                            .build(),
                                    ).build(),
                            ).setRecordedAt(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                            .build()

                    val addResponse = diaryStub.addEntry(addEntryReq)
                    assertNotNull(addResponse.entry.id)
                    assertEquals(EntrySourceType.ENTRY_SOURCE_TYPE_RECIPE, addResponse.entry.sourceType)
                    assertEquals(mealPlanResponse.planId, addResponse.entry.sourceId)
                    println("Запись добавлена в дневник: ID=${addResponse.entry.id}, Тип приема='${addResponse.entry.mealType}', Источник=${addResponse.entry.sourceType}")

                    val dailyEntriesResponse =
                        diaryStub.getDailyEntries(
                            GetDailyEntriesRequest.newBuilder()
                                .setDate("2026-11-24")
                                .setTimeZone("UTC")
                                .build(),
                        )

                    assertEquals(1, dailyEntriesResponse.entriesCount)
                    val entryInDiary = dailyEntriesResponse.entriesList.first()
                    assertEquals(generatedDinner.title, entryInDiary.nameSnapshot)
                    assertEquals(generatedDinner.nutrition.calories.toInt(), dailyEntriesResponse.dailyTotal.calories)
                    assertEquals(generatedDinner.nutrition.proteinsGrams, dailyEntriesResponse.dailyTotal.proteins, 0.1)
                    println("Дневник прочитан: найдено записей=${dailyEntriesResponse.entriesCount}, итого калорий=${dailyEntriesResponse.dailyTotal.calories}, белки=${dailyEntriesResponse.dailyTotal.proteins}г")
                    println("\n ВСЕ 4 ШАГА УСПЕШНО ВЫПОЛНЕНЫ В ЕДИНОЙ ЦЕПОЧКЕ!")
                } finally {
                    recipeChannel?.shutdownNow()
                    recipeServer?.shutdownNow()
                    profileChannel.shutdownNow()
                    profileServer.shutdownNow()
                    catalogChannel.shutdownNow()
                    catalogServer.shutdownNow()
                    diaryChannel.shutdownNow()
                    diaryServer.shutdownNow()
                }
            }
        }

    private fun envelope(text: String): String =
        json.writeValueAsString(
            mapOf(
                "candidates" to listOf(mapOf("finishReason" to "STOP", "content" to mapOf("parts" to listOf(mapOf("text" to text))))),
                "usageMetadata" to mapOf("promptTokenCount" to 100, "candidatesTokenCount" to 50, "thoughtsTokenCount" to 10),
            ),
        )
}
