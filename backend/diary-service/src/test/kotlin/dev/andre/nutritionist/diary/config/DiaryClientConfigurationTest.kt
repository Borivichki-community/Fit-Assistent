package dev.andre.nutritionist.diary.config

import io.grpc.InsecureChannelCredentials
import io.grpc.TlsChannelCredentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class DiaryClientConfigurationTest {
    private val configuration = DiaryClientConfiguration()

    @Test
    fun `plaintext mode creates insecure credentials and channel`() {
        val client =
            DiaryClientProperties.RecipeClient(
                address = "localhost:9194",
                plaintext = true,
            )

        val credentials = configuration.buildRecipeChannelCredentials(client)
        assertInstanceOf(InsecureChannelCredentials::class.java, credentials)

        val channel = configuration.recipeServiceChannel(DiaryClientProperties(clients = DiaryClientProperties.Clients(recipe = client)))
        try {
            assertEquals("localhost:9194", channel.authority())
        } finally {
            channel.shutdownNow()
        }
    }

    @Test
    fun `tls mode without custom files creates default tls credentials`() {
        val client =
            DiaryClientProperties.RecipeClient(
                address = "recipe-ai-service.budget-nutritionist.svc.cluster.local:9194",
                plaintext = false,
            )

        val credentials = configuration.buildRecipeChannelCredentials(client)
        assertInstanceOf(TlsChannelCredentials::class.java, credentials)

        val channel = configuration.recipeServiceChannel(DiaryClientProperties(clients = DiaryClientProperties.Clients(recipe = client)))
        try {
            assertEquals("recipe-ai-service.budget-nutritionist.svc.cluster.local:9194", channel.authority())
        } finally {
            channel.shutdownNow()
        }
    }

    @Test
    fun `tls mode rejects nonexistent ca certificate file`(
        @TempDir tempDir: Path,
    ) {
        val missingCa = tempDir.resolve("missing-ca.crt").toString()
        val client =
            DiaryClientProperties.RecipeClient(
                plaintext = false,
                tlsCaCertPath = missingCa,
            )

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                configuration.buildRecipeChannelCredentials(client)
            }
        assertTrue(ex.message.orEmpty().contains("CA certificate"))
    }

    @Test
    fun `tls mode requires both client certificate and key paths`(
        @TempDir tempDir: Path,
    ) {
        val certFile = tempDir.resolve("tls.crt").apply { writeText("stub") }
        val client =
            DiaryClientProperties.RecipeClient(
                plaintext = false,
                tlsClientCertPath = certFile.toString(),
                tlsClientKeyPath = " ",
            )

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                configuration.buildRecipeChannelCredentials(client)
            }
        assertTrue(ex.message.orEmpty().contains("Both tlsClientCertPath and tlsClientKeyPath"))
    }

    @Test
    fun `tls mode rejects nonexistent client key file`(
        @TempDir tempDir: Path,
    ) {
        val certFile = tempDir.resolve("tls.crt").apply { writeText("stub") }
        val missingKey = tempDir.resolve("missing.key").toString()
        val client =
            DiaryClientProperties.RecipeClient(
                plaintext = false,
                tlsClientCertPath = certFile.toString(),
                tlsClientKeyPath = missingKey,
            )

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                configuration.buildRecipeChannelCredentials(client)
            }
        assertTrue(ex.message.orEmpty().contains("client key file"))
    }
}
