package dev.andre.nutritionist.catalog

import dev.andre.nutritionist.catalog.cache.CatalogCache
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class CatalogCacheFailureTest {
    @Test
    fun unavailableRedisDoesNotPreventDatabaseResult() {
        val client =
            LettuceClientConfiguration
                .builder()
                .commandTimeout(Duration.ofMillis(50))
                .shutdownTimeout(Duration.ZERO)
                .build()
        val connection = LettuceConnectionFactory(RedisStandaloneConfiguration("127.0.0.1", 1), client)
        connection.afterPropertiesSet()
        connection.start()
        val meters = SimpleMeterRegistry()
        try {
            val cache = CatalogCache(StringRedisTemplate(connection), meters, 60)
            val result =
                cache.getOrLoad("product", byteArrayOf(1), { it.toString(Charsets.UTF_8) }, { it.toByteArray() }) {
                    "database-result"
                }
            assertThat(result).isEqualTo("database-result")
            assertThat(
                meters
                    .get("catalog.cache.errors")
                    .tag("operation", "product")
                    .counter()
                    .count(),
            ).isEqualTo(2.0)
            assertThat(
                meters
                    .get("catalog.cache.requests")
                    .tag("operation", "product")
                    .tag("result", "miss")
                    .counter()
                    .count(),
            ).isEqualTo(1.0)
        } finally {
            connection.destroy()
            meters.close()
        }
    }
}
