package dev.andre.nutritionist.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Интеграционные тесты жизнеспособности profile-service.
 *
 * Проверяет:
 * 1. Успешную загрузку Spring ApplicationContext.
 * 2. Доступность /actuator/health/liveness со статусом UP.
 * 3. Доступность /actuator/health/readiness со статусом UP.
 *
 * Внешние зависимости (PostgreSQL, Kafka) отключены,
 * чтобы тест проходил без Docker в CI без Testcontainers-окружения.
 * Полные интеграционные тесты с реальной БД — в отдельном классе (Testcontainers).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [ProfileApplication::class],
)
@ActiveProfiles("dev")
@TestPropertySource(
    properties = [
        "spring.liquibase.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.server.port=0",
        "management.endpoint.health.group.readiness.include=readinessState",
        "management.endpoint.health.group.liveness.include=livenessState",
        "management.endpoint.health.probes.enabled=true",
        "management.health.livenessstate.enabled=true",
        "management.health.readinessstate.enabled=true",
        "management.endpoints.web.exposure.include=health",
    ],
)
class ProfileServiceApplicationTests {
    @LocalManagementPort
    private var managementPort: Int = 0

    private val httpClient = HttpClient.newHttpClient()

    // ── 1. Контекст загружается ───────────────────────────────────────────────

    @Test
    fun `application context loads successfully`() {
        assertTrue(managementPort > 0, "Management port must be assigned")
    }

    // ── 2. Liveness probe ─────────────────────────────────────────────────────

    @Test
    fun `liveness endpoint returns UP`() {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$managementPort/actuator/health/liveness"))
                .GET()
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode(), "Liveness probe must return HTTP 200")
        assertTrue(response.body().contains("\"status\":\"UP\""), "Liveness status must be UP")
    }

    // ── 3. Readiness probe ────────────────────────────────────────────────────

    @Test
    fun `readiness endpoint returns UP`() {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$managementPort/actuator/health/readiness"))
                .GET()
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode(), "Readiness probe must return HTTP 200")
        assertTrue(response.body().contains("\"status\":\"UP\""), "Readiness status must be UP")
    }
}
