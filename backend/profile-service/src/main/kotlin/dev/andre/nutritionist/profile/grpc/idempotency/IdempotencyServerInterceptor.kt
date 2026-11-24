package dev.andre.nutritionist.profile.grpc.idempotency

import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Серверный перехватчик для проверки и фиксации ключа идемпотентности (idempotency-key).
 *
 * Соответствует AGENTS.md §4.4, 02-backend-stack.md §7.1 и spring-grpc-service/SKILL.md §8.
 *
 * Реализует State Machine в Redis:
 * 1. Атомарное занятие ключа (SETNX) со статусом PROCESSING и lock TTL 60с.
 * 2. Хэширование тела запроса (SHA-256) и проверка на несовпадение данных (PayloadMismatch -> INVALID_ARGUMENT).
 * 3. Обнаружение одновременных запросов в полёте (InProgress -> ABORTED с рекомендацией повтора).
 * 4. Фиксация успешного ответа со статусом COMPLETED и TTL 24 часа.
 * 5. Освобождение ключа (release) при сбоях сервиса для чистого retry.
 */
const val IDEMPOTENCY_INTERCEPTOR_ORDER: Int = Ordered.HIGHEST_PRECEDENCE + 10

@Component
@GlobalServerInterceptor
@Order(IDEMPOTENCY_INTERCEPTOR_ORDER)
class IdempotencyServerInterceptor(
    private val idempotencyStore: IdempotencyStore,
) : ServerInterceptor {
    private val logger = LoggerFactory.getLogger(IdempotencyServerInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val key = headers.get(IDEMPOTENCY_KEY_HEADER)
        if (key == null) {
            return next.startCall(call, headers)
        }

        val capturingCall = IdempotencyCapturingCall(call, key, idempotencyStore)
        val delegateListener = next.startCall(capturingCall, headers)
        return IdempotencyServerCallListener(delegateListener, capturingCall, call, key, idempotencyStore, logger)
    }

    private class IdempotencyServerCallListener<ReqT, RespT>(
        delegate: ServerCall.Listener<ReqT>,
        private val capturingCall: IdempotencyCapturingCall<ReqT, RespT>,
        private val serverCall: ServerCall<ReqT, RespT>,
        private val key: String,
        private val store: IdempotencyStore,
        private val logger: org.slf4j.Logger,
    ) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
        private var isShortCircuited = false

        override fun onMessage(message: ReqT) {
            val requestBytes = serializeRequest(message)
            val requestHash = computeSha256(requestBytes)
            capturingCall.requestHash = requestHash

            when (val result = store.tryAcquire(key, requestHash)) {
                is IdempotencyResult.Acquired -> {
                    super.onMessage(message)
                }

                is IdempotencyResult.Cached -> {
                    isShortCircuited = true
                    capturingCall.isCompleted = true
                    logger.info("Idempotent cache hit for key [{}]", key)
                    val marshaller = serverCall.methodDescriptor.responseMarshaller
                    val stream: InputStream = ByteArrayInputStream(result.responseBytes)
                    val cachedResp = marshaller.parse(stream)
                    serverCall.sendHeaders(Metadata())
                    serverCall.sendMessage(cachedResp)
                    serverCall.close(Status.OK, Metadata())
                }

                is IdempotencyResult.InProgress -> {
                    isShortCircuited = true
                    capturingCall.isCompleted = true
                    logger.warn("Concurrent request detected for idempotency-key [{}]", key)
                    val desc = "Concurrent request with idempotency-key [$key] is currently in-flight."
                    serverCall.close(
                        Status.ABORTED.withDescription(desc),
                        Metadata(),
                    )
                }

                is IdempotencyResult.PayloadMismatch -> {
                    isShortCircuited = true
                    capturingCall.isCompleted = true
                    logger.error("Payload mismatch for idempotency-key [{}]", key)
                    val desc = "Idempotency-key [$key] was already used with a different request payload."
                    serverCall.close(
                        Status.INVALID_ARGUMENT.withDescription(desc),
                        Metadata(),
                    )
                }
            }
        }

        override fun onHalfClose() {
            if (!isShortCircuited) {
                super.onHalfClose()
            }
        }

        override fun onCancel() {
            if (!isShortCircuited && !capturingCall.isCompleted) {
                store.release(key)
            }
            super.onCancel()
        }

        @Suppress("UNCHECKED_CAST")
        private fun serializeRequest(message: ReqT): ByteArray {
            val marshaller = serverCall.methodDescriptor.requestMarshaller as MethodDescriptor.Marshaller<ReqT>
            val stream = marshaller.stream(message)
            return stream.readAllBytes()
        }

        private fun computeSha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
    }

    private class IdempotencyCapturingCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>,
        private val key: String,
        private val store: IdempotencyStore,
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
        var requestHash: ByteArray? = null
        var isCompleted: Boolean = false
        private var lastResponseBytes: ByteArray? = null

        override fun sendMessage(message: RespT) {
            val marshaller = methodDescriptor.responseMarshaller

            @Suppress("UNCHECKED_CAST")
            val stream = (marshaller as MethodDescriptor.Marshaller<RespT>).stream(message)
            lastResponseBytes = stream.readAllBytes()
            super.sendMessage(message)
        }

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            if (!isCompleted) {
                val hash = requestHash
                val bytes = lastResponseBytes
                if (status.isOk && hash != null && bytes != null) {
                    store.complete(key, hash, bytes)
                    isCompleted = true
                } else {
                    store.release(key)
                }
            }
            super.close(status, trailers)
        }
    }

    companion object {
        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}
