package dev.andre.nutritionist.profile.grpc.interceptor

import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component

typealias GrpcGlobalServerInterceptor = GlobalServerInterceptor

/**
 * Глобальный серверный перехватчик исключений gRPC (T-020).
 *
 * Перехватывает исключения доменного и персистентного слоёв, транслируя их
 * в канонические gRPC статус-коды:
 * - [EntityNotFoundException], [NoSuchElementException] -> [Status.NOT_FOUND]
 * - [IllegalArgumentException], [IllegalStateException] -> [Status.INVALID_ARGUMENT]
 * - [DataIntegrityViolationException] -> [Status.ALREADY_EXISTS]
 * - [OptimisticLockException], [OptimisticLockingFailureException] ->
 *   [Status.ABORTED] (конфликт конкурентных изменений)
 * - [StatusException], [StatusRuntimeException] -> пропуск без изменений (включая rich error BadRequest)
 * - Прочие непредвиденные [Throwable] -> [Status.INTERNAL] ("Internal server error"),
 *   предотвращая утечку внутренней схемы SQL/стектрейса клиентам.
 */
@Suppress("TooGenericExceptionCaught")
@Component
@GlobalServerInterceptor
@Order(Ordered.HIGHEST_PRECEDENCE)
class GrpcExceptionInterceptor : ServerInterceptor {
    private val logger = LoggerFactory.getLogger(GrpcExceptionInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val wrappedCall = ExceptionTranslatingServerCall(call)
        val listener =
            try {
                next.startCall(wrappedCall, headers)
            } catch (t: Throwable) {
                wrappedCall.closeWithException(t)
                return object : ServerCall.Listener<ReqT>() {}
            }
        return ExceptionTranslatingListener(listener, wrappedCall)
    }

    private inner class ExceptionTranslatingServerCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>,
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
        private var isClosed = false

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            if (isClosed) return
            isClosed = true

            if (status.isOk) {
                super.close(status, trailers)
                return
            }

            val cause = status.cause
            val (translatedStatus, translatedTrailers) =
                if (cause != null) {
                    translateThrowable(cause)
                } else if (isInternalOrUnknown(status.code)) {
                    val userId = MDC.get("userId")
                    logger.error(
                        "gRPC call failed with unexpected status [{}] for user [{}]: {}",
                        status.code,
                        userId,
                        status.description,
                    )
                    Status.INTERNAL.withDescription("Internal server error") to trailers
                } else {
                    status to trailers
                }

            val finalTrailers =
                if (translatedTrailers.keys().isNotEmpty()) {
                    translatedTrailers
                } else {
                    trailers
                }
            super.close(translatedStatus, finalTrailers)
        }

        private fun isInternalOrUnknown(code: Status.Code): Boolean =
            code == Status.Code.UNKNOWN ||
                code == Status.Code.INTERNAL

        fun closeWithException(t: Throwable) {
            if (isClosed) return
            isClosed = true
            val (status, trailers) = translateThrowable(t)
            super.close(status, trailers)
        }
    }

    private inner class ExceptionTranslatingListener<ReqT>(
        delegate: ServerCall.Listener<ReqT>,
        private val call: ExceptionTranslatingServerCall<*, *>,
    ) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
        override fun onHalfClose() {
            try {
                super.onHalfClose()
            } catch (t: Throwable) {
                call.closeWithException(t)
            }
        }

        override fun onMessage(message: ReqT) {
            try {
                super.onMessage(message)
            } catch (t: Throwable) {
                call.closeWithException(t)
            }
        }

        override fun onCancel() {
            try {
                super.onCancel()
            } catch (t: Throwable) {
                call.closeWithException(t)
            }
        }

        override fun onComplete() {
            try {
                super.onComplete()
            } catch (t: Throwable) {
                call.closeWithException(t)
            }
        }

        override fun onReady() {
            try {
                super.onReady()
            } catch (t: Throwable) {
                call.closeWithException(t)
            }
        }
    }

    internal fun translateThrowable(throwable: Throwable): Pair<Status, Metadata> {
        val root = unwrap(throwable)
        val userId = MDC.get("userId")

        return when (root) {
            is StatusException -> {
                root.status to (root.trailers ?: Metadata())
            }

            is StatusRuntimeException -> {
                root.status to (root.trailers ?: Metadata())
            }

            is NoSuchElementException,
            is EntityNotFoundException,
            -> {
                logger.warn("Entity not found for user [{}]: {}", userId, root.message)
                Status.NOT_FOUND.withDescription(root.message ?: "Resource not found") to Metadata()
            }

            is IllegalArgumentException,
            is IllegalStateException,
            -> {
                logger.warn("Invalid argument or state for user [{}]: {}", userId, root.message)
                Status.INVALID_ARGUMENT.withDescription(root.message ?: "Invalid argument") to Metadata()
            }

            is DataIntegrityViolationException -> {
                logger.warn("Data integrity violation for user [{}]: {}", userId, root.message)
                Status.ALREADY_EXISTS.withDescription("Resource conflict or unique constraint violation") to Metadata()
            }

            is OptimisticLockException,
            is OptimisticLockingFailureException,
            -> {
                logger.warn("Optimistic lock failure for user [{}]: {}", userId, root.message)
                Status.ABORTED.withDescription("Concurrent modification detected. Please retry.") to Metadata()
            }

            else -> {
                logger.error("Unexpected error during gRPC call for user [{}]: {}", userId, root.message, root)
                Status.INTERNAL.withDescription("Internal server error") to Metadata()
            }
        }
    }

    private fun unwrap(throwable: Throwable): Throwable {
        var current = throwable
        while (shouldUnwrap(current)) {
            val cause = current.cause
            if (cause == null || cause === current) {
                break
            }
            current = cause
        }
        return current
    }

    private fun shouldUnwrap(t: Throwable): Boolean =
        t !is StatusException &&
            t !is StatusRuntimeException &&
            t !is NoSuchElementException &&
            t !is EntityNotFoundException &&
            t !is IllegalArgumentException &&
            t !is IllegalStateException &&
            t !is DataIntegrityViolationException &&
            t !is OptimisticLockException &&
            t !is OptimisticLockingFailureException &&
            t.cause != null
}
