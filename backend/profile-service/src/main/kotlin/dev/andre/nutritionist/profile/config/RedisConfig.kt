package dev.andre.nutritionist.profile.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Конфигурация Redis для хранения сериализованных gRPC-ответов идемпотентности.
 */
@Configuration
class RedisConfig {
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, ByteArray> {
        val template = RedisTemplate<String, ByteArray>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = RedisSerializer.byteArray()
        return template
    }
}
