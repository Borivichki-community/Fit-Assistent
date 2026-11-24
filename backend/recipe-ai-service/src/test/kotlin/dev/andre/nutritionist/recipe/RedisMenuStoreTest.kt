package dev.andre.nutritionist.recipe

import dev.andre.nutritionist.recipe.config.RecipeConfiguration
import dev.andre.nutritionist.recipe.config.RecipeProperties
import dev.andre.nutritionist.recipe.domain.model.CatalogProduct
import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.model.ProductId
import dev.andre.nutritionist.recipe.domain.model.UserId
import dev.andre.nutritionist.recipe.domain.validator.MenuValidator
import dev.andre.nutritionist.recipe.infrastructure.llm.QuotaExceeded
import dev.andre.nutritionist.recipe.infrastructure.redis.RedisMenuStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Testcontainers(disabledWithoutDocker = true)
class RedisMenuStoreTest {
    @Test
    fun `Redis stores warmup menu and atomically caps concurrent quota attempts`() {
        val config = RecipeConfiguration()
        val properties = RecipeProperties(dailyAttemptLimit = 3)
        val json = JsonMapper.builder().build()
        val connection = LettuceConnectionFactory(redis.host, redis.getMappedPort(6379))
        connection.afterPropertiesSet()
        try {
            val template = StringRedisTemplate(connection)
            val store = RedisMenuStore(template, properties, Clock.systemUTC(), config.recipeIoDispatcher(), json)
            val product = CatalogProduct(ProductId("rice"), "Рис", 200.0, 7.0, 1.0, 40.0, 150L)
            val menu =
                MenuValidator(json, properties)
                    .validate(RecipeLlmTest.menuJson(), MenuRequest(600, 5000), listOf(product))
            runBlocking {
                assertTrue(store.beginWarmup("600:5000"))
                assertFalse(store.beginWarmup("600:5000"))
                store.write("menu", menu, Duration.ofMinutes(1))
                assertEquals(menu, store.read("menu"))

                val user = UserId(UUID.randomUUID().toString())
                val successful =
                    (1..12)
                        .map {
                            async {
                                try {
                                    store.reserveAttempt(user)
                                    true
                                } catch (_: QuotaExceeded) {
                                    false
                                }
                            }
                        }.awaitAll()
                        .count { it }
                assertEquals(3, successful)
            }
        } finally {
            connection.destroy()
        }
    }

    companion object {
        @Container
        @JvmField
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply { addExposedPort(6379) }
    }
}
