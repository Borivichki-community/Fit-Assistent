package dev.andre.nutritionist.catalog.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, ByteArray> {
        val template = RedisTemplate<String, ByteArray>()
        template.connectionFactory = connectionFactory
        val stringSerializer = StringRedisSerializer()
        val byteArraySerializer = RedisSerializer.byteArray()

        template.keySerializer = stringSerializer
        template.valueSerializer = byteArraySerializer
        template.hashKeySerializer = stringSerializer
        template.hashValueSerializer = byteArraySerializer
        template.afterPropertiesSet()
        return template
    }
}
