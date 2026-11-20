package dev.andre.nutritionist.catalog.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.andre.nutritionist.catalog.cache.model.CachedAveragePriceDto
import dev.andre.nutritionist.catalog.cache.model.CachedProductDto
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID

@Service
class CatalogCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val redisObjectMapper: ObjectMapper,
    meterRegistry: MeterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry? = null,
) {
    private val logger = LoggerFactory.getLogger(CatalogCacheService::class.java)

    private val circuitBreaker = circuitBreakerRegistry?.circuitBreaker("catalog-redis-cache")

    private val productHitCounter: Counter =
        Counter
            .builder("cache_hits_total")
            .tag("cache", "catalog_product")
            .description("Total number of cache hits for product card")
            .register(meterRegistry)

    private val productMissCounter: Counter =
        Counter
            .builder("cache_misses_total")
            .tag("cache", "catalog_product")
            .description("Total number of cache misses for product card")
            .register(meterRegistry)

    private val avgPricesHitCounter: Counter =
        Counter
            .builder("cache_hits_total")
            .tag("cache", "catalog_avg_prices")
            .description("Total number of cache hits for average prices")
            .register(meterRegistry)

    private val avgPricesMissCounter: Counter =
        Counter
            .builder("cache_misses_total")
            .tag("cache", "catalog_avg_prices")
            .description("Total number of cache misses for average prices")
            .register(meterRegistry)

    companion object {
        val PRODUCT_TTL: Duration = Duration.ofHours(1)
        val AVG_PRICES_TTL: Duration = Duration.ofHours(6)

        private const val PRODUCT_KEY_PREFIX = "catalog:product"
        private const val AVG_PRICES_KEY_PREFIX = "catalog:prices:avg"

        fun formatProductKey(
            productId: UUID,
            regionId: UUID?,
        ): String {
            val reg = regionId?.toString() ?: "all"
            return "$PRODUCT_KEY_PREFIX:$productId:$reg"
        }

        fun formatAvgPricesKey(
            productIds: List<UUID>,
            regionId: UUID?,
        ): String {
            val sortedIds = productIds.map { it.toString() }.sorted().joinToString(",")
            val hash = sha256Hex(sortedIds)
            val reg = regionId?.toString() ?: "all"
            return "$AVG_PRICES_KEY_PREFIX:$hash:$reg"
        }

        private fun sha256Hex(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun getProduct(
        productId: UUID,
        regionId: UUID?,
    ): CachedProductDto? {
        val key = formatProductKey(productId, regionId)
        return executeSafe(
            operationName = "getProduct",
            onMiss = { productMissCounter.increment() },
        ) {
            val raw = redisTemplate.opsForValue().get(key)
            if (raw != null) {
                val dto = redisObjectMapper.convertValue(raw, CachedProductDto::class.java)
                productHitCounter.increment()
                dto
            } else {
                productMissCounter.increment()
                null
            }
        }
    }

    fun putProduct(
        productId: UUID,
        regionId: UUID?,
        product: CachedProductDto,
    ) {
        val key = formatProductKey(productId, regionId)
        executeSafeVoid("putProduct") {
            redisTemplate.opsForValue().set(key, product, PRODUCT_TTL)
        }
    }

    fun getAveragePrices(
        productIds: List<UUID>,
        regionId: UUID?,
    ): List<CachedAveragePriceDto>? {
        if (productIds.isEmpty()) return emptyList()
        val key = formatAvgPricesKey(productIds, regionId)
        return executeSafe(
            operationName = "getAveragePrices",
            onMiss = { avgPricesMissCounter.increment() },
        ) {
            val raw = redisTemplate.opsForValue().get(key)
            if (raw != null) {
                val typeRef = object : TypeReference<List<CachedAveragePriceDto>>() {}
                val dtos = redisObjectMapper.convertValue(raw, typeRef)
                avgPricesHitCounter.increment()
                dtos
            } else {
                avgPricesMissCounter.increment()
                null
            }
        }
    }

    fun putAveragePrices(
        productIds: List<UUID>,
        regionId: UUID?,
        prices: List<CachedAveragePriceDto>,
    ) {
        if (productIds.isEmpty()) return
        val key = formatAvgPricesKey(productIds, regionId)
        executeSafeVoid("putAveragePrices") {
            redisTemplate.opsForValue().set(key, prices, AVG_PRICES_TTL)
        }
    }

    fun evictProduct(productId: UUID) {
        executeSafeVoid("evictProduct") {
            // Evict all region variations for this product
            val productPattern = "$PRODUCT_KEY_PREFIX:$productId:*"
            val productKeys = redisTemplate.keys(productPattern)
            if (!productKeys.isNullOrEmpty()) {
                redisTemplate.delete(productKeys)
            }

            // Evict all average prices as regional price update alters averages
            val avgPricesPattern = "$AVG_PRICES_KEY_PREFIX:*"
            val avgPriceKeys = redisTemplate.keys(avgPricesPattern)
            if (!avgPriceKeys.isNullOrEmpty()) {
                redisTemplate.delete(avgPriceKeys)
            }
        }
    }

    fun evictAll() {
        executeSafeVoid("evictAll") {
            val keys = redisTemplate.keys("catalog:*")
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
            }
        }
    }

    private fun <T> executeSafe(
        operationName: String,
        onMiss: (() -> Unit)? = null,
        block: () -> T?,
    ): T? =
        try {
            if (circuitBreaker != null) {
                circuitBreaker.executeSupplier(block)
            } else {
                block()
            }
        } catch (ex: Throwable) {
            logger.warn(
                "Redis cache operation '$operationName' failed (circuit breaker open or network outage). " +
                    "Degrading gracefully to database: {}",
                ex.message,
            )
            onMiss?.invoke()
            null
        }

    private fun executeSafeVoid(
        operationName: String,
        block: () -> Unit,
    ) {
        try {
            if (circuitBreaker != null) {
                circuitBreaker.executeRunnable(block)
            } else {
                block()
            }
        } catch (ex: Throwable) {
            logger.warn(
                "Redis cache write operation '$operationName' failed. Proceeding without cache: {}",
                ex.message,
            )
        }
    }
}
