package dev.andre.nutritionist.catalog.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig {
    @Bean
    fun circuitBreakerRegistry(environment: Environment): CircuitBreakerRegistry {
        val settings =
            Binder
                .get(environment)
                .bind("resilience4j.circuitbreaker.instances.catalog-redis-cache", CacheCircuitBreakerSettings::class.java)
                .get()
        val config =
            CircuitBreakerConfig
                .custom()
                .slidingWindowSize(settings.slidingWindowSize)
                .minimumNumberOfCalls(settings.minimumNumberOfCalls)
                .failureRateThreshold(settings.failureRateThreshold)
                .waitDurationInOpenState(settings.waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(settings.permittedNumberOfCallsInHalfOpenState)
                .automaticTransitionFromOpenToHalfOpenEnabled(settings.automaticTransitionFromOpenToHalfOpenEnabled)
                .recordExceptions(*settings.recordExceptions.toTypedArray())
                .build()

        return CircuitBreakerRegistry.of(config)
    }

    data class CacheCircuitBreakerSettings(
        val slidingWindowSize: Int,
        val minimumNumberOfCalls: Int,
        val failureRateThreshold: Float,
        val waitDurationInOpenState: Duration,
        val permittedNumberOfCallsInHalfOpenState: Int,
        val automaticTransitionFromOpenToHalfOpenEnabled: Boolean,
        val recordExceptions: List<Class<out Throwable>>,
    )

    @Bean
    fun redisObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerKotlinModule()
        return mapper
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper,
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        val stringSerializer = StringRedisSerializer()
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)

        template.keySerializer = stringSerializer
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = stringSerializer
        template.hashValueSerializer = jsonSerializer
        template.afterPropertiesSet()
        return template
    }
}
