package dev.andre.nutritionist.diary.service

import dev.andre.nutritionist.diary.config.DiaryClientProperties
import dev.andre.nutritionist.diary.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.diary.grpc.client.RecipeAccessClient
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class CoachAccessCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val recipeAccessClient: RecipeAccessClient,
    private val properties: DiaryClientProperties,
    private val dispatchers: CoroutineDispatchers,
    meterRegistryProvider: ObjectProvider<MeterRegistry>,
) {
    private val logger = LoggerFactory.getLogger(CoachAccessCacheService::class.java)

    private val hitCounter: Counter? =
        meterRegistryProvider.ifAvailable?.let { registry ->
            Counter
                .builder("diary_coach_access_cache_total")
                .tag("result", "hit")
                .description("Total Redis cache hits for coach access")
                .register(registry)
        }

    private val missCounter: Counter? =
        meterRegistryProvider.ifAvailable?.let { registry ->
            Counter
                .builder("diary_coach_access_cache_total")
                .tag("result", "miss")
                .description("Total Redis cache misses for coach access")
                .register(registry)
        }

    private val errorCounter: Counter? =
        meterRegistryProvider.ifAvailable?.let { registry ->
            Counter
                .builder("diary_coach_access_cache_total")
                .tag("result", "error")
                .description("Total Redis cache errors for coach access")
                .register(registry)
        }

    suspend fun hasCoachAccess(
        coachId: String,
        studentId: String,
    ): Boolean {
        val key = formatKey(coachId, studentId)
        val cached = readCached(key)
        if (cached != null) {
            return cached
        }

        // Fail-Closed: on gRPC/CircuitBreaker failure this throws Status.UNAVAILABLE without writing to Redis.
        val hasAccess = recipeAccessClient.checkCoachAccess(coachId, studentId)
        writeCached(key, hasAccess)
        return hasAccess
    }

    private suspend fun readCached(key: String): Boolean? =
        try {
            val raw = withContext(dispatchers.io) { redisTemplate.opsForValue().get(key) }
            when (raw) {
                VALUE_ALLOWED -> {
                    hitCounter?.increment()
                    true
                }

                VALUE_DENIED -> {
                    hitCounter?.increment()
                    false
                }

                else -> {
                    missCounter?.increment()
                    null
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorCounter?.increment()
            logger.warn("Redis read failed for key={}, degrading transparently to recipe-ai-service: {}", key, error.message)
            null
        }

    private suspend fun writeCached(
        key: String,
        hasAccess: Boolean,
    ) {
        val value = if (hasAccess) VALUE_ALLOWED else VALUE_DENIED
        try {
            withContext(dispatchers.io) {
                redisTemplate.opsForValue().set(key, value, properties.coachAccess.cacheTtl)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorCounter?.increment()
            logger.warn("Redis write failed for key={}, continuing without caching: {}", key, error.message)
        }
    }

    companion object {
        private const val KEY_PREFIX = "diary:coach_access"
        private const val VALUE_ALLOWED = "1"
        private const val VALUE_DENIED = "0"

        fun formatKey(
            coachId: String,
            studentId: String,
        ): String = "$KEY_PREFIX:$coachId:$studentId"
    }
}
