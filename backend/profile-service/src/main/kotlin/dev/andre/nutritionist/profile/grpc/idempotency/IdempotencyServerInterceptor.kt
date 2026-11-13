package dev.andre.nutritionist.profile.grpc.idempotency

import io.grpc.ForwardingServerCall
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

/**
 * Серверный перехватчик для проверки и фиксации ключа идемпотентности (idempotency-key).
 *
 * Соответствует AGENTS.md §4.4, 02-backend-stack.md §7.1 и spring-grpc-service/SKILL.md §8.
 * При наличии ключа в заголовке gRPC и попадании в кэш — возвращает сохранённый ответ
 * без повторного обращения к БД и без повторного выполнения бизнес-логики.
 */
@Component
@GlobalServerInterceptor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
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
        if (key != null) {
            val cachedBytes = idempotencyStore.get(key)
            if (cachedBytes != null) {
                logger.info("Idempotent cache hit for key [{}]", key)
                val marshaller = call.methodDescriptor.responseMarshaller
                val stream: InputStream = ByteArrayInputStream(cachedBytes)
                val cachedResp = marshaller.parse(stream)
                call.sendMessage(cachedResp)
                call.close(Status.OK, headers)
                return object : ServerCall.Listener<ReqT>() {}
            }
        }

        val capturingCall = IdempotencyCapturingCall(call, key, idempotencyStore)
        return next.startCall(capturingCall, headers)
    }

    private class IdempotencyCapturingCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>,
        private val idempotencyKey: String?,
        private val idempotencyStore: IdempotencyStore,
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
        private var lastResponseBytes: ByteArray? = null

        override fun sendMessage(message: RespT) {
            if (idempotencyKey != null) {
                val marshaller = methodDescriptor.responseMarshaller

                @Suppress("UNCHECKED_CAST")
                val stream = (marshaller as MethodDescriptor.Marshaller<RespT>).stream(message)
                lastResponseBytes = stream.readAllBytes()
            }
            super.sendMessage(message)
        }

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            if (status.isOk && idempotencyKey != null) {
                val bytes = lastResponseBytes
                if (bytes != null) {
                    idempotencyStore.put(idempotencyKey, bytes)
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
