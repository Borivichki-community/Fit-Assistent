package dev.andre.nutritionist.diary.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.ManagedChannel
import io.grpc.TlsChannelCredentials
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DiaryClientProperties::class)
class DiaryClientConfiguration {
    @Bean(name = ["recipeServiceChannel"], destroyMethod = "shutdownNow")
    fun recipeServiceChannel(properties: DiaryClientProperties): ManagedChannel {
        val recipe = properties.clients.recipe
        val credentials =
            if (recipe.plaintext) {
                InsecureChannelCredentials.create()
            } else {
                TlsChannelCredentials.newBuilder().build()
            }
        return Grpc.newChannelBuilder(recipe.address, credentials).build()
    }

    @Bean
    fun diaryCircuitBreakerRegistry(
        properties: DiaryClientProperties,
        meterRegistryProvider: ObjectProvider<MeterRegistry>,
    ): CircuitBreakerRegistry {
        val recipe = properties.clients.recipe
        val config =
            CircuitBreakerConfig
                .custom()
                .slidingWindowSize(recipe.breakerWindow)
                .minimumNumberOfCalls(recipe.breakerMinCalls)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(recipe.breakerOpenFor)
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build()
        return CircuitBreakerRegistry.of(config).also { registry ->
            registry.circuitBreaker(RECIPE_ACCESS_CIRCUIT_BREAKER)
            meterRegistryProvider.ifAvailable?.let { meterRegistry ->
                TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry)
            }
        }
    }

    companion object {
        const val RECIPE_ACCESS_CIRCUIT_BREAKER = "diary-recipe-access"
    }
}
