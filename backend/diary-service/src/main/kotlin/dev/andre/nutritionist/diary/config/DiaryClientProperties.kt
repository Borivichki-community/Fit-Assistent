package dev.andre.nutritionist.diary.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("diary")
class DiaryClientProperties(
    val clients: Clients = Clients(),
    val coachAccess: CoachAccess = CoachAccess(),
) {
    class Clients(
        val recipe: RecipeClient = RecipeClient(),
    )

    class RecipeClient(
        val address: String = "localhost:9194",
        val plaintext: Boolean = true,
        val devBypass: Boolean = true,
        val tlsCaCertPath: String? = null,
        val tlsClientCertPath: String? = null,
        val tlsClientKeyPath: String? = null,
        val deadline: Duration = Duration.ofSeconds(2),
        val breakerWindow: Int = 10,
        val breakerMinCalls: Int = 5,
        val breakerOpenFor: Duration = Duration.ofSeconds(30),
    ) {
        init {
            require(address.isNotBlank()) { "diary.clients.recipe.address must not be blank" }
            require(deadline.toMillis() > 0) { "diary.clients.recipe.deadline must be positive" }
            require(breakerMinCalls in 1..breakerWindow) { "breakerMinCalls must be in 1..breakerWindow" }
            require(breakerOpenFor.toMillis() > 0) { "diary.clients.recipe.breaker-open-for must be positive" }
        }
    }

    class CoachAccess(
        val cacheTtl: Duration = Duration.ofSeconds(60),
    ) {
        init {
            require(cacheTtl.toSeconds() > 0) { "diary.coach-access.cache-ttl must be positive" }
        }
    }
}
