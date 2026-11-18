package dev.andre.nutritionist.catalog.cache

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64

@Component
class CatalogCache(
    private val redis: StringRedisTemplate,
    private val registry: MeterRegistry,
    @param:Value("\${catalog.cache.ttl-seconds:60}") private val ttlSeconds: Long,
) {
    init {
        require(ttlSeconds in 1..3600) { "Catalog cache TTL must be 1..3600 seconds" }
    }

    fun <T> getOrLoad(
        operation: String,
        material: ByteArray,
        decode: (ByteArray) -> T,
        encode: (T) -> ByteArray,
        load: () -> T,
    ): T {
        val hash = MessageDigest.getInstance("SHA-256").digest(material).joinToString("") { "%02x".format(it) }
        val key = "catalog:v1:$operation:$hash"
        val cached =
            try {
                redis.opsForValue().get(key)?.let { decode(Base64.getDecoder().decode(it)) }
            } catch (exception: Exception) {
                failure(operation, exception)
                null
            }
        if (cached != null) {
            registry.counter("catalog.cache.requests", "operation", operation, "result", "hit").increment()
            return cached
        }
        registry.counter("catalog.cache.requests", "operation", operation, "result", "miss").increment()
        val result = load()
        try {
            redis.opsForValue().set(key, Base64.getEncoder().encodeToString(encode(result)), Duration.ofSeconds(ttlSeconds))
        } catch (exception: Exception) {
            failure(operation, exception)
        }
        return result
    }

    private fun failure(
        operation: String,
        exception: Exception,
    ) {
        registry.counter("catalog.cache.errors", "operation", operation).increment()
        LoggerFactory.getLogger(CatalogCache::class.java).debug("Catalog cache unavailable", exception)
    }
}
