package dev.andre.nutritionist.profile.grpc.idempotency

import com.budgetntr.proto.profile.v1.ProfileServiceGrpc
import dev.andre.nutritionist.profile.grpc.security.SecurityGrpcInterceptor
import dev.andre.nutritionist.profile.grpc.security.currentAuthContext
import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/**
 * Серверный перехватчик для проверки и фиксации ключа идемпотентности (idempotency-key).
 *
 * Соответствует AGENTS.md §4.4, 02-backend-stack.md §7.1 и spring-grpc-service/SKILL.md §8.
 * При наличии ключа в заголовке gRPC и попадании в кэш — возвращает сохранённый ответ
 * без повторного обращения к БД и без повторного выполнения бизнес-логики.
 */
const val IDEMPOTENCY_INTERCEPTOR_ORDER: Int = SecurityGrpcInterceptor.ORDER_PRIORITY + 10

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
        val auth = currentAuthContext()
        val userId = auth.userId
        if (
            key == null || !auth.isAuthenticated || userId.isNullOrBlank() ||
            call.methodDescriptor.fullMethodName != ProfileServiceGrpc.getUpdateProfileMethod().fullMethodName
        ) {
            return next.startCall(call, headers)
        }

        val scopedKey = scopedKey(userId, key)
        val capturingCall = IdempotencyCapturingCall(call, scopedKey, idempotencyStore)
        val delegateListener = next.startCall(capturingCall, headers)
        return IdempotencyServerCallListener(delegateListener, capturingCall, call, scopedKey, idempotencyStore, logger)
    }

    private class IdempotencyServerCallListener<ReqT, RespT>(
        delegate: ServerCall.Listener<ReqT>,
        private val capturingCall: IdempotencyCapturingCall<ReqT, RespT>,
        private val serverCall: ServerCall<ReqT, RespT>,
        private val key: String,
        private val store: IdempotencyStore,
        private val logger: org.slf4j.Logger,
    ) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
        override fun onMessage(message: ReqT) {
            val requestBytes = serializeRequest(message)
            val requestHash = computeSha256(requestBytes)
            when (val result = store.tryAcquire(key, requestHash)) {
                IdempotencyResult.Acquired -> {
                    capturingCall.requestHash.set(requestHash)
                    super.onMessage(message)
                }
                is IdempotencyResult.Cached -> {
                    logger.info("Idempotent cache hit for a completed request")
                    val marshaller = serverCall.methodDescriptor.responseMarshaller
                    val cachedResp = result.responseBytes.inputStream().use(marshaller::parse)
                    serverCall.sendHeaders(Metadata())
                    serverCall.sendMessage(cachedResp)
                    serverCall.close(Status.OK, Metadata())
                }
                IdempotencyResult.InProgress -> {
                    logger.warn("Concurrent request detected for UpdateProfile")
                    val desc = "Concurrent request with this idempotency-key is currently in-flight."
                    serverCall.close(
                        Status.ABORTED.withDescription(desc),
                        Metadata(),
                    )
                }
                IdempotencyResult.PayloadMismatch -> {
                    logger.error("Payload mismatch for UpdateProfile")
                    val desc = "This idempotency-key was already used with a different request payload."
                    serverCall.close(
                        Status.INVALID_ARGUMENT.withDescription(desc),
                        Metadata(),
                    )
                }
            }
        }

        override fun onHalfClose() {
            if (capturingCall.requestHash.get() != null) {
                super.onHalfClose()
            }
        }

        private fun serializeRequest(message: ReqT): ByteArray =
            serverCall.methodDescriptor.requestMarshaller.stream(message).use { it.readAllBytes() }

        private fun computeSha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
    }

    private class IdempotencyCapturingCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>,
        private val key: String,
        private val store: IdempotencyStore,
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
        val requestHash = AtomicReference<ByteArray>()
        private val lastResponseBytes = AtomicReference<ByteArray>()

        override fun sendMessage(message: RespT) {
            val responseBytes = methodDescriptor.responseMarshaller.stream(message).use { it.readAllBytes() }
            lastResponseBytes.set(responseBytes)
            super.sendMessage(message)
        }

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            val hash = requestHash.getAndSet(null)
            try {
                if (hash != null) {
                    val bytes = lastResponseBytes.get()
                    if (status.isOk && bytes != null) {
                        store.complete(key, hash, bytes)
                    } else {
                        store.release(key)
                    }
                }
            } finally {
                super.close(status, trailers)
            }
        }
    }

    companion object {
        internal fun scopedKey(userId: String, key: String): String = "profile-update:${userId.length}:$userId:$key"

        val IDEMPOTENCY_KEY_HEADER: Metadata.Key<String> =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }
}
