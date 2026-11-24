package dev.andre.nutritionist.recipe.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Duration

@ConfigurationProperties("recipe")
class RecipeProperties(
    val catalogTarget: String = "localhost:9192",
    val catalogPlaintext: Boolean = false,
    val catalogCaFile: Path? = null,
    val catalogDeadline: Duration = Duration.ofSeconds(2),
    val model: String = "gemini-2.5-flash",
    val llmBaseUrl: String = "https://generativelanguage.googleapis.com",
    val llmApiKey: String = "",
    val llmTimeout: Duration = Duration.ofSeconds(12),
    val llmMaxAttempts: Int = 2,
    val llmRetryDelay: Duration = Duration.ofMillis(250),
    val llmMaxOutputTokens: Int = 8192,
    val breakerWindow: Int = 10,
    val breakerMinCalls: Int = 5,
    val breakerOpenFor: Duration = Duration.ofSeconds(30),
    val calorieTolerance: Double = 0.10,
    val dailyAttemptLimit: Int = 10,
    val cacheTtl: Duration = Duration.ofHours(24),
    val fallbackTtl: Duration = Duration.ofDays(2),
    val warmupTargets: List<String> = emptyList(),
    val inputUsdPerMillion: BigDecimal? = null,
    val outputUsdPerMillion: BigDecimal? = null,
) {
    init {
        require(catalogDeadline.toMillis() > 0 && llmTimeout.toMillis() > 0 && llmRetryDelay.toMillis() > 0)
        require(llmMaxAttempts in 1..3 && llmMaxOutputTokens in 1024..16_384)
        require(breakerMinCalls in 1..breakerWindow && breakerOpenFor.toMillis() > 0)
        require(calorieTolerance in 0.0..0.2 && dailyAttemptLimit > 0)
        require(cacheTtl.toSeconds() > 0 && fallbackTtl.toSeconds() > 0 && warmupTargets.size <= 25)
        require(listOfNotNull(inputUsdPerMillion, outputUsdPerMillion).all { it.signum() >= 0 })
    }
}
