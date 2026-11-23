package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.WeekMenu
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import java.util.Base64

@Component
class RedisMenuStore(
    private val redis: StringRedisTemplate,
    private val settings: RecipeProperties,
    private val clock: Clock,
    private val recipeIoDispatcher: CoroutineDispatcher,
) {
    suspend fun read(key: String): WeekMenu? =
        optionalRedis {
            val value = redis.opsForValue().get(key) ?: return@optionalRedis null
            try {
                WeekMenu.parseFrom(Base64.getDecoder().decode(value)).takeIf { it.daysCount == WEEK_DAYS && it.hasSummary() }
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: InvalidProtocolBufferException) {
                null
            }
        }

    suspend fun write(
        key: String,
        menu: WeekMenu,
        ttl: Duration,
    ) {
        optionalRedis { redis.opsForValue().set(key, Base64.getEncoder().encodeToString(menu.toByteArray()), ttl) }
    }

    suspend fun beginWarmup(key: String): Boolean =
        optionalRedis { redis.opsForValue().setIfAbsent("recipe:warmup:$key", "1", Duration.ofMinutes(10)) } == true

    suspend fun reserveAttempt(user: UserId) {
        val now = clock.instant()
        val date = now.atZone(ZoneOffset.UTC).toLocalDate()
        val nextDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val expiry = Duration.between(now, nextDay).seconds + 1
        val key = "recipe:quota:$date:${digest(user.raw)}"
        val allowed =
            try {
                withContext(recipeIoDispatcher) {
                    redis.execute(reserve, listOf(key), settings.dailyAttemptLimit.toString(), expiry.toString())
                }
            } catch (_: DataAccessException) {
                throw MenuUnavailable("Generation quota storage is unavailable")
            }
        when (allowed) {
            1L -> Unit
            0L -> throw QuotaExceeded()
            else -> throw MenuUnavailable("Generation quota storage is unavailable")
        }
    }

    private suspend fun <T> optionalRedis(operation: () -> T): T? =
        withContext(recipeIoDispatcher) {
            try {
                operation()
            } catch (_: DataAccessException) {
                null
            }
        }

    companion object {
        private val reserve =
            DefaultRedisScript(
                """
                local used = tonumber(redis.call('GET', KEYS[1]) or '0')
                if used >= tonumber(ARGV[1]) then return 0 end
                if used == 0 then
                    redis.call('SET', KEYS[1], '1', 'EX', ARGV[2])
                else
                    redis.call('INCR', KEYS[1])
                end
                return 1
                """.trimIndent(),
                Long::class.javaObjectType,
            )
    }
}
