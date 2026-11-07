package dev.andre.nutritionist.data.remote.grpc.auth

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class AuthCallCredentials(
    private val tokenProvider: AuthTokenProvider,
    private val dispatchers: CoroutineDispatchers,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io),
) : CallCredentials() {

    override fun applyRequestMetadata(
        requestInfo: RequestInfo,
        appExecutor: Executor,
        applier: MetadataApplier,
    ) {
        coroutineScope.launch(dispatchers.io) {
            try {
                val refreshedResult = tokenProvider.refreshIfNeeded()
                val token = refreshedResult.getOrNull() ?: tokenProvider.getAccessToken()

                val headers = Metadata()
                if (!token.isNullOrBlank()) {
                    headers.put(AUTHORIZATION_METADATA_KEY, "Bearer $token")
                }
                appExecutor.execute {
                    applier.apply(headers)
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                appExecutor.execute {
                    applier.fail(
                        Status.UNAUTHENTICATED
                            .withDescription("Failed to apply authentication metadata: ${e.message}")
                            .withCause(e),
                    )
                }
            }
        }
    }

    @Deprecated("Required by gRPC CallCredentials abstract class", level = DeprecationLevel.WARNING)
    override fun thisUsesUnstableApi() {}

    companion object {
        val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    }
}
