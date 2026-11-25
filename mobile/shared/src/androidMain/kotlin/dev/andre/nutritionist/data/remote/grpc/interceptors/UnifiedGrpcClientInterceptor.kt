package dev.andre.nutritionist.data.remote.grpc.interceptors

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.coroutines.DefaultCoroutineDispatchers
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.domain.trace.TraceContextHolder
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Единый gRPC Client Interceptor.
 * Автоматически инжектирует в метаданные каждого исходящего RPC-вызова:
 * 1. authorization: Bearer <token> (из AuthTokenProvider);
 * 2. traceparent: 00-<traceId>-<spanId>-01 (из TraceContextHolder);
 * 3. idempotency-key: <uuid> (если задан в Context или если RPC-метод мутирующий);
 * 4. Автоматический однократный refresh OAuth-токена (под Mutex) при Status.UNAUTHENTICATED.
 */
class UnifiedGrpcClientInterceptor(
    private val tokenProvider: AuthTokenProvider? = null,
    private val traceContextHolder: TraceContextHolder? = null,
    private val userIdProvider: (() -> String?)? = null,
    private val idempotencyKeyProvider: (() -> String?)? = null,
    private val devBypass: Boolean = false,
    dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers(),
) : ClientInterceptor {

    private val refreshMutex = Mutex()
    private val refreshScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        val stableIdempotencyKey = resolveIdempotencyKey(method.fullMethodName, callOptions)
        val initialDelegate = next.newCall(method, callOptions)
        return RetryingClientCall(
            method = method,
            callOptions = callOptions,
            next = next,
            initialDelegate = initialDelegate,
            stableIdempotencyKey = stableIdempotencyKey,
        )
    }

    private inner class RetryingClientCall<ReqT, RespT>(
        private val method: MethodDescriptor<ReqT, RespT>,
        private val callOptions: CallOptions,
        private val next: Channel,
        initialDelegate: ClientCall<ReqT, RespT>,
        private val stableIdempotencyKey: String?,
    ) : ClientCall<ReqT, RespT>() {

        private val lock = Any()
        private var delegate: ClientCall<ReqT, RespT> = initialDelegate
        private var outerListener: Listener<RespT>? = null
        private val originalHeaders = Metadata()
        private val sentMessages = mutableListOf<ReqT>()
        private var requestedMessages = 0
        private var halfClosed = false
        private var messageCompression: Boolean? = null
        private var cancelled = false

        override fun start(responseListener: Listener<RespT>, headers: Metadata) {
            val attemptToken: String?
            val attemptHeaders = Metadata()
            synchronized(lock) {
                outerListener = responseListener
                originalHeaders.merge(headers)
                attemptHeaders.merge(originalHeaders)
                attemptToken = currentAccessToken()
                injectHeaders(attemptHeaders, attemptToken, stableIdempotencyKey)
            }
            delegate.start(
                AttemptListener(attemptToken = attemptToken, isRetry = false),
                attemptHeaders,
            )
        }

        override fun request(numMessages: Int) {
            val active = synchronized(lock) {
                requestedMessages += numMessages
                delegate
            }
            active.request(numMessages)
        }

        override fun cancel(message: String?, cause: Throwable?) {
            val active = synchronized(lock) {
                cancelled = true
                delegate
            }
            active.cancel(message, cause)
        }

        override fun halfClose() {
            val active = synchronized(lock) {
                halfClosed = true
                delegate
            }
            active.halfClose()
        }

        override fun sendMessage(message: ReqT) {
            val active = synchronized(lock) {
                sentMessages.add(message)
                delegate
            }
            active.sendMessage(message)
        }

        override fun setMessageCompression(enabled: Boolean) {
            val active = synchronized(lock) {
                messageCompression = enabled
                delegate
            }
            active.setMessageCompression(enabled)
        }

        override fun isReady(): Boolean = synchronized(lock) { delegate }.isReady

        private inner class AttemptListener(
            private val attemptToken: String?,
            private val isRetry: Boolean,
        ) : Listener<RespT>() {

            private var pendingHeaders: Metadata? = null
            private var hasEmittedMessage = false

            override fun onHeaders(headers: Metadata) {
                if (isRetry) {
                    outerListener?.onHeaders(headers)
                } else {
                    pendingHeaders = Metadata().apply { merge(headers) }
                }
            }

            override fun onMessage(message: RespT) {
                hasEmittedMessage = true
                flushPendingHeaders()
                outerListener?.onMessage(message)
            }

            override fun onReady() {
                outerListener?.onReady()
            }

            override fun onClose(status: Status, trailers: Metadata) {
                val canRetry = synchronized(lock) { !cancelled } &&
                    !isRetry &&
                    !hasEmittedMessage &&
                    status.code == Status.Code.UNAUTHENTICATED &&
                    tokenProvider != null

                if (!canRetry) {
                    flushPendingHeaders()
                    outerListener?.onClose(status, trailers)
                    return
                }

                refreshScope.launch {
                    val refreshResult = refreshAccessToken(attemptToken)
                    if (synchronized(lock) { cancelled }) {
                        return@launch
                    }

                    refreshResult.fold(
                        onSuccess = { newToken ->
                            val retryDelegate = next.newCall(method, callOptions)
                            val retryHeaders = Metadata().apply { merge(originalHeaders) }
                            injectHeaders(retryHeaders, newToken, stableIdempotencyKey)

                            val replay = synchronized(lock) {
                                if (cancelled) {
                                    retryDelegate.cancel("Coroutine cancelled", null)
                                    return@launch
                                }
                                delegate = retryDelegate
                                ReplaySnapshot(
                                    requested = requestedMessages,
                                    messages = sentMessages.toList(),
                                    shouldHalfClose = halfClosed,
                                    compression = messageCompression,
                                )
                            }

                            retryDelegate.start(
                                AttemptListener(attemptToken = newToken, isRetry = true),
                                retryHeaders,
                            )
                            replay.compression?.let { retryDelegate.setMessageCompression(it) }
                            if (replay.requested > 0) {
                                retryDelegate.request(replay.requested)
                            }
                            for (msg in replay.messages) {
                                retryDelegate.sendMessage(msg)
                            }
                            if (replay.shouldHalfClose) {
                                retryDelegate.halfClose()
                            }
                        },
                        onFailure = { error ->
                            if (error is CancellationException) return@launch
                            flushPendingHeaders()
                            val failureStatus = Status.UNAUTHENTICATED
                                .withDescription(error.message ?: "Authentication refresh failed")
                                .withCause(error)
                            outerListener?.onClose(failureStatus, trailers)
                        },
                    )
                }
            }

            private fun flushPendingHeaders() {
                val buffered = pendingHeaders ?: return
                pendingHeaders = null
                outerListener?.onHeaders(buffered)
            }
        }
    }

    private data class ReplaySnapshot<ReqT>(
        val requested: Int,
        val messages: List<ReqT>,
        val shouldHalfClose: Boolean,
        val compression: Boolean?,
    )

    private suspend fun refreshAccessToken(failedToken: String?): Result<String> {
        val provider = tokenProvider
            ?: return Result.failure(DomainError.Unauthorized("AuthTokenProvider is not configured"))

        return refreshMutex.withLock {
            val current = runCatching { provider.getAccessToken() }.getOrNull()
            if (!current.isNullOrBlank() && current != failedToken) {
                Result.success(current)
            } else {
                provider.refreshIfNeeded().mapCatching { refreshed ->
                    if (refreshed.isBlank()) {
                        throw DomainError.Unauthorized("Refreshed token is blank")
                    }
                    refreshed
                }
            }
        }
    }

    private fun currentAccessToken(): String? =
        tokenProvider?.let { provider ->
            runCatching { provider.getAccessToken() }.getOrNull()
        }

    private fun resolveIdempotencyKey(
        fullMethodName: String,
        callOptions: CallOptions,
    ): String? {
        val optionKey = callOptions.getOption(IDEMPOTENCY_KEY_CALL_OPTION)
        val contextKey = IDEMPOTENCY_KEY_CTX.get()
        val providedKey = idempotencyKeyProvider?.invoke()
        val candidate = optionKey ?: contextKey ?: providedKey
        return when {
            !candidate.isNullOrBlank() -> candidate
            isMutatingMethod(fullMethodName) -> generateUuid()
            else -> null
        }
    }

    private fun injectHeaders(
        headers: Metadata,
        accessToken: String?,
        idempotencyKey: String?,
    ) {
        if (!accessToken.isNullOrBlank()) {
            headers.put(AUTHORIZATION_METADATA_KEY, "Bearer $accessToken")
        }

        val traceparent = traceContextHolder?.getTraceparent() ?: generateTraceparent()
        headers.put(TRACEPARENT_METADATA_KEY, traceparent)

        if (!idempotencyKey.isNullOrBlank()) {
            headers.put(IDEMPOTENCY_METADATA_KEY, idempotencyKey)
        }

        val userId = userIdProvider?.invoke()
        if (!userId.isNullOrBlank()) {
            headers.put(USER_ID_METADATA_KEY, userId)
        }

        if (devBypass) {
            headers.put(DEV_BYPASS_METADATA_KEY, "true")
        }
    }

    private fun isMutatingMethod(fullMethodName: String): Boolean {
        val methodName = fullMethodName.substringAfterLast('/')
        return methodName.startsWith("Add") ||
            methodName.startsWith("Create") ||
            methodName.startsWith("Update") ||
            methodName.startsWith("Delete") ||
            methodName.startsWith("Upsert") ||
            methodName.startsWith("Fork") ||
            methodName.startsWith("Publish") ||
            methodName.startsWith("Invite") ||
            methodName.startsWith("Respond") ||
            methodName.startsWith("Grant") ||
            methodName.startsWith("Revoke")
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

        val USER_ID_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)

        val DEV_BYPASS_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-dev-bypass", Metadata.ASCII_STRING_MARSHALLER)

        val IDEMPOTENCY_KEY_CTX: Context.Key<String> = Context.key("idempotency-key")

        val IDEMPOTENCY_KEY_CALL_OPTION: CallOptions.Key<String> =
            CallOptions.Key.create<String>("idempotency-key")
    }
}
