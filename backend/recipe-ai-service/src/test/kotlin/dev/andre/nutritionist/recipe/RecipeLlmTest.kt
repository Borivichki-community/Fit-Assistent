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
            val cost = meters.get("recipe.llm.request.cost.usd").summary()
            assertEquals(1L, cost.count())
            assertEquals(0.00016, cost.totalAmount(), 0.00000001)
        }
    }

    @Test
    fun `invalid JSON is retried once and outside budget is rejected`() {
        wire.stubFor(
            post(PATH)
                .inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("valid")
                .willReturn(okJson(envelope("{bad"))),
        )
        wire.stubFor(post(PATH).inScenario("retry").whenScenarioStateIs("valid").willReturn(okJson(envelope(menuJson()))))
        withClient { generator, _ ->
            val result = runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} }
            assertEquals(7, result.daysCount)
            wire.verify(2, postRequestedFor(urlEqualTo(PATH)))
        }
        val validator = MenuValidator(json, settings())
        val lowBudget = REQUEST.copy(weeklyBudgetMinor = 3000)
        assertThrows(InvalidMenu::class.java) {
            validator.validate(menuJson(), lowBudget, listOf(PRODUCT))
        }
        assertThrows(InvalidMenu::class.java) {
            validator.validate(menuJson(grams = 125), REQUEST, listOf(PRODUCT))
        }
        assertThrows(InvalidMenu::class.java) {
            validator.validate(menuJson(id = "unknown"), REQUEST, listOf(PRODUCT))
        }
    }

    @Test
    fun `open breaker blocks provider calls then closes after successful probes`() {
        wire.stubFor(post(PATH).willReturn(aResponse().withStatus(500)))
        withClient(minCalls = 2) { generator, breaker ->
            val transitions = mutableListOf<CircuitBreaker.State>()
            breaker.eventPublisher.onStateTransition { transitions.add(it.stateTransition.toState) }
            assertThrows(ProviderFailure::class.java) { runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} } }
            assertEquals(CircuitBreaker.State.OPEN, breaker.state)
            assertThrows(CallNotPermittedException::class.java) { runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} } }
            wire.verify(2, postRequestedFor(urlEqualTo(PATH)))
            wire.resetAll()
            wire.stubFor(post(PATH).willReturn(okJson(envelope(menuJson()))))

            breaker.transitionToHalfOpenState()
            runBlocking { repeat(2) { generator.generate(REQUEST, listOf(PRODUCT), null) {} } }
            assertEquals(CircuitBreaker.State.CLOSED, breaker.state)
            assertEquals(listOf(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN, CircuitBreaker.State.CLOSED), transitions)
            assertTrue(meters.find("resilience4j.circuitbreaker.state").gauges().isNotEmpty())
        }
    }

    @Test
    fun `slow response can complete within limit`() {
        wire.stubFor(post(PATH).willReturn(okJson(envelope(menuJson())).withFixedDelay(80)))
        withClient { generator, _ ->
            val result = runBlocking { generator.generate(REQUEST, listOf(PRODUCT), null) {} }
            assertEquals(7, result.daysCount)
        }
    }

    @Test
    fun `timeout aborts both stalled attempts`() {
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
                    json,
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
        private val PRODUCT = CatalogProduct(ProductId("rice"), "Рис", 200.0, 150)
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
