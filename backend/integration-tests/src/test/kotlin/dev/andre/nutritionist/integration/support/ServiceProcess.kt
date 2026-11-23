package dev.andre.nutritionist.integration.support

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

class ServiceProcess(
    name: String,
    properties: Map<String, String>,
) : AutoCloseable {
    private val port = ServerSocket(0).use { it.localPort }
    private val log = Path.of(System.getProperty("integration.logs"), "$name.log")
    private val process: Process =
        run {
            Files.createDirectories(log.parent)
            ProcessBuilder(
                listOf(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Xmx256m",
                    "-jar",
                    System.getProperty("integration.$name.jar"),
                    "--spring.grpc.server.port=$port",
                    "--spring.grpc.server.address=127.0.0.1",
                    "--server.port=0",
                    "--management.server.port=0",
                ) + properties.map { (key, value) -> "--$key=$value" },
            ).redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start()
        }

    val channel: ManagedChannel =
        ManagedChannelBuilder
            .forAddress("127.0.0.1", port)
            .usePlaintext()
            .build()

    fun awaitReady() {
        try {
            await()
                .atMost(Duration.ofSeconds(45))
                .pollInterval(Duration.ofMillis(200))
                .ignoreException(StatusRuntimeException::class.java)
                .untilAsserted {
                    check(process.isAlive) { "Service exited: $log" }
                    val response =
                        HealthGrpc
                            .newBlockingStub(channel)
                            .withDeadlineAfter(1, TimeUnit.SECONDS)
                            .check(HealthCheckRequest.getDefaultInstance())
                    assertEquals(HealthCheckResponse.ServingStatus.SERVING, response.status)
                }
        } catch (failure: Exception) {
            val tail = Files.readAllLines(log).takeLast(50).joinToString("\n")
            throw IllegalStateException("Service did not become ready: $log\n$tail", failure)
        }
    }

    override fun close() {
        try {
            channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS)
        } finally {
            process.destroy()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(5, TimeUnit.SECONDS)
            }
        }
    }
}
