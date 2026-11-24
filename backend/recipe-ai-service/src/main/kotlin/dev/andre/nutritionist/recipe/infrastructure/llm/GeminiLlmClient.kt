package dev.andre.nutritionist.recipe.infrastructure.llm

import dev.andre.nutritionist.recipe.config.RecipeProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class GeminiLlmClient(
    private val http: HttpClient,
    private val json: JsonMapper,
    private val settings: RecipeProperties,
    metrics: MeterRegistry,
) : LlmClient {
    private val inputTokens = metrics.counter("recipe.llm.tokens", "direction", "input")
    private val outputTokens = metrics.counter("recipe.llm.tokens", "direction", "output")
    private val cost = metrics.summary("recipe.llm.request.cost.usd")
    private val attempts = metrics.counter("recipe.llm.attempts")

    override suspend fun generate(
        prompt: String,
        traceparent: String?,
    ): String {
        if (settings.llmApiKey.isBlank()) throw MenuUnavailable("LLM API key is unavailable")
        val payload =
            mapOf(
                "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))),
                "generationConfig" to
                    mapOf(
                        "responseMimeType" to "application/json",
                        "maxOutputTokens" to settings.llmMaxOutputTokens,
                    ),
            )
        val endpoint = "${settings.llmBaseUrl.trimEnd('/')}/v1beta/models/${settings.model}:generateContent"
        val request =
            HttpRequest
                .newBuilder(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", settings.llmApiKey)
                .apply { traceparent?.let { header("traceparent", it) } }
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                .build()

        attempts.increment()
        val response =
            try {
                http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            } catch (_: IOException) {
                throw ProviderFailure(true, "LLM transport failure")
            }
        val status = response.statusCode()
        if (status != 200) throw ProviderFailure(status == 408 || status == 429 || status in 500..599, "LLM HTTP $status")

        val body =
            try {
                json.readTree(response.body())
            } catch (_: JacksonException) {
                throw InvalidMenu("Provider returned malformed response")
            }
        recordUsage(body.path("usageMetadata"))
        val candidate = body.path("candidates").path(0)
        if (candidate.path("finishReason").asString("") != "STOP") throw InvalidMenu("LLM did not complete a menu")
        return candidate
            .path("content")
            .path("parts")
            .filterNot { it.path("thought").asBoolean(false) }
            .joinToString("") { it.path("text").asString("") }
            .takeIf { it.isNotBlank() && it.length <= 100_000 }
            ?: throw InvalidMenu("LLM returned empty or oversized menu")
    }

    private fun recordUsage(usage: JsonNode) {
        val input = usage.path("promptTokenCount").asLong(0).coerceAtLeast(0)
        val output =
            maxOf(
                usage.path("candidatesTokenCount").asLong(0) + usage.path("thoughtsTokenCount").asLong(0),
                usage.path("totalTokenCount").asLong(0) - input,
                0,
            )
        inputTokens.increment(input.toDouble())
        outputTokens.increment(output.toDouble())
        val inputPrice = settings.inputUsdPerMillion ?: return
        val outputPrice = settings.outputUsdPerMillion ?: return
        cost.record((inputPrice * input.toBigDecimal() + outputPrice * output.toBigDecimal()).movePointLeft(6).toDouble())
    }
}

class InvalidMenu(
    message: String,
) : RuntimeException(message)
