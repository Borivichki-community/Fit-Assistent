package dev.andre.nutritionist.profile

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@ActiveProfiles("dev")
abstract class AbstractPostgresIntegrationTest {
    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withDatabaseName("profile_test_db")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(true)
                .apply { start() }

        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true)
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.liquibase.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }
}
