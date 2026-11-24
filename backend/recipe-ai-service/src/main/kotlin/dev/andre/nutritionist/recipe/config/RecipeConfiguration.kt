package dev.andre.nutritionist.recipe.config

import dev.andre.nutritionist.recipe.infrastructure.llm.InvalidMenu
import dev.andre.nutritionist.recipe.infrastructure.llm.MenuUnavailable
import dev.andre.nutritionist.recipe.infrastructure.llm.ProviderFailure
import dev.andre.nutritionist.recipe.infrastructure.llm.QuotaExceeded
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.ManagedChannel
import io.grpc.TlsChannelCredentials
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.net.http.HttpClient
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeoutException

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RecipeProperties::class)
@EnableScheduling
class RecipeConfiguration {
    @Bean(destroyMethod = "shutdownNow")
    fun catalogChannel(settings: RecipeProperties): ManagedChannel {
        val credentials =
            if (settings.catalogPlaintext) {
                InsecureChannelCredentials.create()
            } else {
                val builder = TlsChannelCredentials.newBuilder()
                settings.catalogCaFile?.let { builder.trustManager(it.toFile()) }
                builder.build()
            }
        return Grpc.newChannelBuilder(settings.catalogTarget, credentials).build()
    }

    @Bean(destroyMethod = "close")
    fun llmHttpClient(): HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()

    @Bean(destroyMethod = "")
    fun recipeIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Bean
    fun recipeClock(): Clock = Clock.systemUTC()

    @Bean
    fun recipeCircuitBreakers(
        settings: RecipeProperties,
        meters: MeterRegistry,
    ): CircuitBreakerRegistry {
        val config =
            CircuitBreakerConfig
                .custom()
                .slidingWindowSize(settings.breakerWindow)
                .minimumNumberOfCalls(settings.breakerMinCalls)
                .failureRateThreshold(50f)
                .slowCallDurationThreshold(settings.llmTimeout.dividedBy(2))
                .slowCallRateThreshold(75f)
                .waitDurationInOpenState(settings.breakerOpenFor)
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(QuotaExceeded::class.java, MenuUnavailable::class.java)
                .build()
        return CircuitBreakerRegistry.of(config).also { TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(it).bindTo(meters) }
    }

    @Bean
    fun recipeRetries(settings: RecipeProperties): RetryRegistry =
        RetryRegistry.of(
            RetryConfig
                .custom<Any>()
                .maxAttempts(settings.llmMaxAttempts)
                .intervalFunction { attempt -> settings.llmRetryDelay.toMillis() * (1L shl (attempt - 1)) }
                .retryOnException { failure ->
                    failure is InvalidMenu || failure is TimeoutException || (failure is ProviderFailure && failure.retryable)
                }.build(),
        )

    @Bean
    fun recipeTimeLimiter(settings: RecipeProperties): TimeLimiter =
        TimeLimiter.of("llm", TimeLimiterConfig.custom().timeoutDuration(settings.llmTimeout).build())
}
