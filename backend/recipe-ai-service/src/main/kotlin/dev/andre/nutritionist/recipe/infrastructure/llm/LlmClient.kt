package dev.andre.nutritionist.recipe.infrastructure.llm

fun interface LlmClient {
    suspend fun generate(
        prompt: String,
        traceparent: String?,
    ): String
}

class ProviderFailure(
    val retryable: Boolean,
    message: String,
) : RuntimeException(message)

class MenuUnavailable(
    message: String,
) : RuntimeException(message)

class QuotaExceeded : RuntimeException("Daily generation quota exhausted")
