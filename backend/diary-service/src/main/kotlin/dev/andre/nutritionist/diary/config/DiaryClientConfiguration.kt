package dev.andre.nutritionist.diary.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import io.grpc.ManagedChannel
import io.grpc.TlsChannelCredentials
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DiaryClientProperties::class)
class DiaryClientConfiguration {
    @Bean(name = ["recipeServiceChannel"], destroyMethod = "shutdownNow")
    fun recipeServiceChannel(properties: DiaryClientProperties): ManagedChannel {
        val recipe = properties.clients.recipe
        val credentials = buildRecipeChannelCredentials(recipe)
        return Grpc.newChannelBuilder(recipe.address, credentials).build()
    }

    internal fun buildRecipeChannelCredentials(recipe: DiaryClientProperties.RecipeClient): ChannelCredentials {
        if (recipe.plaintext) {
            return InsecureChannelCredentials.create()
        }
        val tlsBuilder = TlsChannelCredentials.newBuilder()
        val caPath = recipe.tlsCaCertPath?.takeIf { it.isNotBlank() }
        if (caPath != null) {
            val caFile = File(caPath)
            require(caFile.isFile) { "Recipe gRPC CA certificate file does not exist: $caPath" }
            tlsBuilder.trustManager(caFile)
        }
        val certPath = recipe.tlsClientCertPath?.takeIf { it.isNotBlank() }
        val keyPath = recipe.tlsClientKeyPath?.takeIf { it.isNotBlank() }
        if (certPath != null || keyPath != null) {
            require(certPath != null && keyPath != null) {
                "Both tlsClientCertPath and tlsClientKeyPath must be configured for mTLS"
            }
            val certFile = File(certPath)
            val keyFile = File(keyPath)
            require(certFile.isFile) { "Recipe gRPC client certificate file does not exist: $certPath" }
            require(keyFile.isFile) { "Recipe gRPC client key file does not exist: $keyPath" }
            tlsBuilder.keyManager(certFile, keyFile)
        }
        return tlsBuilder.build()
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
