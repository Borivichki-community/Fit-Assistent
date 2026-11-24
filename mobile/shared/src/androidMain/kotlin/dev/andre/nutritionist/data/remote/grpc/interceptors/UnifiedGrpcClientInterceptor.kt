package dev.andre.nutritionist.data.remote.grpc.interceptors

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.trace.TraceContextHolder
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.Context
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Единый gRPC Client Interceptor.
 * Автоматически инжектирует в метаданные каждого исходящего RPC-вызова:
 * 1. authorization: Bearer <token> (из AuthTokenProvider);
 * 2. traceparent: 00-<traceId>-<spanId>-01 (из TraceContextHolder);
 * 3. idempotency-key: <uuid> (если задан в Context или если RPC-метод мутирующий).
 */
class UnifiedGrpcClientInterceptor(
    private val tokenProvider: AuthTokenProvider? = null,
    private val traceContextHolder: TraceContextHolder? = null,
) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                // 1. Authorization: Bearer <token>
                if (tokenProvider != null) {
                    val token = runCatching { tokenProvider.getAccessToken() }.getOrNull()
                    if (!token.isNullOrBlank()) {
                        headers.put(AUTHORIZATION_METADATA_KEY, "Bearer $token")
                    }
                }

                // 2. Traceparent: 00-<traceId>-<spanId>-01
                val traceparent = traceContextHolder?.getTraceparent() ?: generateTraceparent()
                headers.put(TRACEPARENT_METADATA_KEY, traceparent)

                // 3. Idempotency-Key
                val contextKey = IDEMPOTENCY_KEY_CTX.get()
                if (!contextKey.isNullOrBlank()) {
                    headers.put(IDEMPOTENCY_METADATA_KEY, contextKey)
                } else if (isMutatingMethod(method.fullMethodName)) {
                    val generated = generateUuid()
                    headers.put(IDEMPOTENCY_METADATA_KEY, generated)
                }

                super.start(responseListener, headers)
            }
        }
    }

    private fun isMutatingMethod(fullMethodName: String): Boolean {
        val methodName = fullMethodName.substringAfterLast('/')
        return methodName.startsWith("Add") ||
            methodName.startsWith("Create") ||
            methodName.startsWith("Update") ||
            methodName.startsWith("Delete") ||
            methodName.startsWith("Upsert")
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateUuid(): String = Uuid.random().toString()

    private fun generateTraceparent(): String {
        val traceId = buildString(32) {
            val bytes = Random.nextBytes(16)
            for (b in bytes) {
                append(b.toUByte().toString(16).padStart(2, '0'))
            }
        }
        val spanId = buildString(16) {
            val bytes = Random.nextBytes(8)
            for (b in bytes) {
                append(b.toUByte().toString(16).padStart(2, '0'))
            }
        }
        return "00-$traceId-$spanId-01"
    }

    companion object {
        val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

        val TRACEPARENT_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)

        val IDEMPOTENCY_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)

        val IDEMPOTENCY_KEY_CTX: Context.Key<String> = Context.key("idempotency-key")
    }
}
