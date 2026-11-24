package dev.andre.nutritionist.recipe

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import dev.andre.nutritionist.recipe.config.RecipeConfiguration
import dev.andre.nutritionist.recipe.config.RecipeProperties
import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.ProductId
import dev.andre.nutritionist.recipe.domain.service.ResilientMenuGenerator
import dev.andre.nutritionist.recipe.domain.validator.MenuValidator
import dev.andre.nutritionist.recipe.infrastructure.llm.GeminiLlmClient
import dev.andre.nutritionist.recipe.infrastructure.llm.prompt.MealPlanPromptBuilder
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.concurrent.TimeoutException

class RecipeLlmTest {
    @RegisterExtension
    @JvmField
    val wire = WireMockExtension.newInstance().options(options().dynamicPort()).build()

    private val json = JsonMapper.builder().build()
    private val meters = SimpleMeterRegistry()
    private val configuration = RecipeConfiguration()

    @Test
    fun `structured success calculates totals and meters provider usage`() {
        wire.stubFor(
            post(PATH)
                .withHeader("x-goog-api-key", equalTo("test-key"))
                .withHeader("traceparent", equalTo(TRACE))
                .willReturn(okJson(envelope(menuJson()))),
        )
        withClient { generator, _ ->
            val result = runBlocking { generator.generate(REQUEST, listOf(PRODUCT), TRACE) {} }

            assertEquals((1..7).toList(), result.daysList.map { it.dayNumber })
            assertEquals(4200.0, result.summary.calories)
            assertEquals(3150L, result.summary.estimatedCostMinor)
            assertEquals(
                100.0,
                meters
                    .get("recipe.llm.tokens")
                    .tag("direction", "input")
                    .counter()
                    .count(),
            )
            assertEquals(
                30.0,
                meters
                    .get("recipe.llm.tokens")
                    .tag("direction", "output")
                    .counter()
                    .count(),
            )
            assertEquals(
                0.00016,
                meters
                    .get("recipe.llm.request.cost.usd")
                    .summary()
                    .totalAmount(),
                0.000001,
            )
            assertEquals(
                1.0,
                meters
                    .get("recipe.llm.attempts")
                    .counter()
                    .count(),
            )
        }
    }

    @Test
    fun `breaker opens after repeated server errors and fast fails further attempts`() {
        wire.stubFor(post(PATH).willReturn(aResponse().withStatus(500).withBody("upstream down")))
        withClient(minCalls = 2) { generator, breaker ->
            assertThrows(RuntimeException::class.java) { runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} } }
            assertEquals(CircuitBreaker.State.OPEN, breaker.state)
            assertThrows(CallNotPermittedException::class.java) {
                runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} }
            }
            wire.verify(2, postRequestedFor(urlEqualTo(PATH)))
        }
    }

    @Test
    fun `retry recovers from transient provider error`() {
        wire.stubFor(
            post(PATH)
                .inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503).withBody("temporary issue"))
                .willSetStateTo("second"),
        )
        wire.stubFor(
            post(PATH)
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(okJson(envelope(menuJson()))),
        )
        withClient { generator, breaker ->
            val result = runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} }
            assertTrue(result.daysCount == 7)
            wire.verify(2, postRequestedFor(urlEqualTo(PATH)))
            assertEquals(1, breaker.metrics.numberOfFailedCalls)
            assertEquals(CircuitBreaker.State.CLOSED, breaker.state)
        }
    }

    @Test
    fun `empty and malformed candidate payloads trigger retry then fail safely`() {
        wire.stubFor(post(PATH).willReturn(okJson(envelope("{malformed}"))))
        withClient { generator, breaker ->
            assertThrows(RuntimeException::class.java) { runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} } }
            wire.verify(2, postRequestedFor(urlEqualTo(PATH)))
            assertEquals(2, breaker.metrics.numberOfFailedCalls)
        }
    }

    @Test
    fun `timeout interrupts slow generations and counts against breaker`() {
        wire.stubFor(post(PATH).willReturn(okJson(envelope(menuJson())).withFixedDelay(1800)))
        withClient(timeoutMillis = 500) { generator, breaker ->
            assertThrows(TimeoutException::class.java) { runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} } }
            wire.verify(2, postRequestedFor(urlEqualTo(PATH)))
            assertEquals(2, breaker.metrics.numberOfFailedCalls)
        }
    }

    private fun withClient(
        minCalls: Int = 5,
        timeoutMillis: Long = 3_000,
        action: (ResilientMenuGenerator, CircuitBreaker) -> Unit,
    ) {
        val settings = settings(minCalls, timeoutMillis)
        val breakers = configuration.recipeCircuitBreakers(settings, meters)
        configuration.llmHttpClient().use { http ->
            val generator =
                ResilientMenuGenerator(
                    GeminiLlmClient(http, json, settings, meters),
                    MenuValidator(json, settings),
                    MealPlanPromptBuilder(json),
                    breakers,
                    configuration.recipeRetries(settings),
                    configuration.recipeTimeLimiter(settings),
                )
            action(generator, breakers.circuitBreaker("llm"))
        }
    }

    private fun settings(
        minCalls: Int = 5,
        timeoutMillis: Long = 3_000,
    ) = RecipeProperties(
        llmApiKey = "test-key",
        llmBaseUrl = wire.baseUrl(),
        llmTimeout = Duration.ofMillis(timeoutMillis),
        llmRetryDelay = Duration.ofMillis(10),
        breakerMinCalls = minCalls,
        inputUsdPerMillion = 1.toBigDecimal(),
        outputUsdPerMillion = 2.toBigDecimal(),
    )

    private fun envelope(text: String): String =
        json.writeValueAsString(
            mapOf(
                "candidates" to listOf(mapOf("finishReason" to "STOP", "content" to mapOf("parts" to listOf(mapOf("text" to text))))),
                "usageMetadata" to mapOf("promptTokenCount" to 100, "candidatesTokenCount" to 20, "thoughtsTokenCount" to 10),
            ),
        )

    companion object {
        private const val PATH = "/v1beta/models/gemini-2.5-flash:generateContent"
        private const val TRACE = "00-11111111111111111111111111111111-2222222222222222-01"
        private val PRODUCT = CatalogProduct(ProductId("rice"), "Рис", 200.0, pricePer100gMinor = 150)
        private val REQUEST = MenuRequest(600, 5000)

        internal fun menuJson(
            id: String = "rice",
            grams: Int = 100,
        ): String =
            (1..7).joinToString(prefix = """{"days":[""", postfix = "]}") { day ->
                val meals =
                    listOf("breakfast", "lunch", "dinner").joinToString { type ->
                        """{"type":"$type","name":"Рис","products":[{"id":"$id","grams":$grams}]}"""
                    }
                """{"day":$day,"meals":[$meals]}"""
            }
    }
}
