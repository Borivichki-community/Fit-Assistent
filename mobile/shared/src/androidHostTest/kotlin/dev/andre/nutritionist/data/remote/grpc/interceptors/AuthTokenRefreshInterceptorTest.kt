package dev.andre.nutritionist.data.remote.grpc.interceptors

import dev.andre.nutritionist.data.remote.grpc.GrpcErrorMapper
import dev.andre.nutritionist.data.remote.grpc.unaryCall
import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.error.DomainError
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AuthTokenRefreshInterceptorTest {

    private data class RecordedAttempt(
        val authHeader: String?,
        val idempotencyKey: String?,
        val requestPayload: String?,
    )

    private class FakeRefreshingTokenProvider(
        initialToken: String?,
        private val refreshBlock: suspend () -> Result<String>,
    ) : AuthTokenProvider {
        @Volatile
        var currentToken: String? = initialToken
        val refreshCallCount = AtomicInteger(0)

        override fun getAccessToken(): String? = currentToken

        override fun isAuthenticated(): Boolean = !currentToken.isNullOrBlank()

        override suspend fun refreshIfNeeded(): Result<String> {
            refreshCallCount.incrementAndGet()
            val result = refreshBlock()
            result.onSuccess { newToken ->
                currentToken = newToken
            }
            return result
        }
    }

    private class ScriptedChannel(
        private val handler: (RecordedAttempt) -> Pair<Status, String?>,
    ) : Channel() {

        val attempts: MutableList<RecordedAttempt> = Collections.synchronizedList(mutableListOf())

        override fun authority(): String = "localhost"

        override fun <RequestT, ResponseT> newCall(
            methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
            callOptions: CallOptions,
        ): ClientCall<RequestT, ResponseT> {
            return object : ClientCall<RequestT, ResponseT>() {
                private lateinit var listener: Listener<ResponseT>
                private var capturedAuth: String? = null
                private var capturedKey: String? = null
                private var sentPayload: String? = null

                override fun start(responseListener: Listener<ResponseT>, headers: Metadata) {
                    listener = responseListener
                    capturedAuth = headers.get(UnifiedGrpcClientInterceptor.AUTHORIZATION_METADATA_KEY)
                    capturedKey = headers.get(UnifiedGrpcClientInterceptor.IDEMPOTENCY_METADATA_KEY)
                }

                override fun request(numMessages: Int) {}

                override fun cancel(message: String?, cause: Throwable?) {}

                override fun sendMessage(message: RequestT) {
                    sentPayload = message?.toString()
                }

                override fun halfClose() {
                    val attempt = RecordedAttempt(
                        authHeader = capturedAuth,
                        idempotencyKey = capturedKey,
                        requestPayload = sentPayload,
                    )
                    attempts.add(attempt)
                    val (status, responseBody) = handler(attempt)
                    if (status.isOk) {
                        listener.onHeaders(Metadata())
                        @Suppress("UNCHECKED_CAST")
                        listener.onMessage((responseBody ?: "") as ResponseT)
                        listener.onClose(Status.OK, Metadata())
                    } else {
                        listener.onClose(status, Metadata())
                    }
                }
            }
        }
    }

    private val stringMarshaller = object : MethodDescriptor.Marshaller<String> {
        override fun stream(value: String): InputStream = ByteArrayInputStream(value.toByteArray())
        override fun parse(stream: InputStream): String = String(stream.readBytes())
    }

    private val addEntryMethod: MethodDescriptor<String, String> =
        MethodDescriptor.newBuilder<String, String>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("com.budgetntr.diary.v1.DiaryService/AddEntry")
            .setRequestMarshaller(stringMarshaller)
            .setResponseMarshaller(stringMarshaller)
            .build()

    private fun intercept(channel: Channel, interceptor: UnifiedGrpcClientInterceptor): Channel =
        object : Channel() {
            override fun authority(): String = channel.authority()
            override fun <ReqT, RespT> newCall(
                methodDescriptor: MethodDescriptor<ReqT, RespT>,
                callOptions: CallOptions,
            ): ClientCall<ReqT, RespT> = interceptor.interceptCall(methodDescriptor, callOptions, channel)
        }

    @Test
    fun `expired token triggers refreshIfNeeded and retries once preserving idempotency key`() = runTest {
        val tokenProvider = FakeRefreshingTokenProvider(initialToken = "expired-jwt") {
            Result.success("fresh-jwt-999")
        }
        val backendChannel = ScriptedChannel { attempt ->
            if (attempt.authHeader == "Bearer fresh-jwt-999") {
                Status.OK to "entry-saved"
            } else {
                Status.UNAUTHENTICATED.withDescription("JWT expired") to null
            }
        }
        val interceptor = UnifiedGrpcClientInterceptor(
            tokenProvider = tokenProvider,
            dispatchers = TestCoroutineDispatchers(UnconfinedTestDispatcher(testScheduler)),
        )
        val clientChannel = intercept(backendChannel, interceptor)

        val response = clientChannel.unaryCall(
            method = addEntryMethod,
            request = "meal-payload-1",
            callOptions = CallOptions.DEFAULT.withOption(
                UnifiedGrpcClientInterceptor.IDEMPOTENCY_KEY_CALL_OPTION,
                "idem-key-123",
            ),
        )

        assertEquals("entry-saved", response)
        assertEquals(1, tokenProvider.refreshCallCount.get())
        assertEquals(2, backendChannel.attempts.size)

        val first = backendChannel.attempts[0]
        val second = backendChannel.attempts[1]
        assertEquals("Bearer expired-jwt", first.authHeader)
        assertEquals("Bearer fresh-jwt-999", second.authHeader)
        assertEquals("idem-key-123", first.idempotencyKey)
        assertEquals("idem-key-123", second.idempotencyKey)
        assertEquals("meal-payload-1", first.requestPayload)
        assertEquals("meal-payload-1", second.requestPayload)
    }

    @Test
    fun `persistent UNAUTHENTICATED after single retry stops without loop and maps to Unauthorized`() = runTest {
        val tokenProvider = FakeRefreshingTokenProvider(initialToken = "expired-jwt") {
            Result.success("still-rejected-jwt")
        }
        val backendChannel = ScriptedChannel {
            Status.UNAUTHENTICATED.withDescription("Invalid signature") to null
        }
        val interceptor = UnifiedGrpcClientInterceptor(
            tokenProvider = tokenProvider,
            dispatchers = TestCoroutineDispatchers(UnconfinedTestDispatcher(testScheduler)),
        )
        val clientChannel = intercept(backendChannel, interceptor)

        val callResult = runCatching {
            clientChannel.unaryCall(addEntryMethod, "payload")
        }

        assertTrue(callResult.isFailure)
        val exception = callResult.exceptionOrNull()
        assertTrue(exception is StatusRuntimeException)
        assertEquals(Status.Code.UNAUTHENTICATED, exception.status.code)
        assertTrue(GrpcErrorMapper.map(exception) is DomainError.Unauthorized)
        assertEquals(1, tokenProvider.refreshCallCount.get())
        assertEquals(2, backendChannel.attempts.size)
    }

    @Test
    fun `failed refreshIfNeeded skips retry and propagates UNAUTHENTICATED`() = runTest {
        val tokenProvider = FakeRefreshingTokenProvider(initialToken = "expired-jwt") {
            Result.failure(DomainError.Unauthorized("Refresh token revoked"))
        }
        val backendChannel = ScriptedChannel {
            Status.UNAUTHENTICATED.withDescription("JWT expired") to null
        }
        val interceptor = UnifiedGrpcClientInterceptor(
            tokenProvider = tokenProvider,
            dispatchers = TestCoroutineDispatchers(UnconfinedTestDispatcher(testScheduler)),
        )
        val clientChannel = intercept(backendChannel, interceptor)

        val callResult = runCatching {
            clientChannel.unaryCall(addEntryMethod, "payload")
        }

        assertTrue(callResult.isFailure)
        val exception = callResult.exceptionOrNull() as? StatusRuntimeException
        assertNotNull(exception)
        assertEquals(Status.Code.UNAUTHENTICATED, exception.status.code)
        assertTrue(GrpcErrorMapper.map(exception) is DomainError.Unauthorized)
        assertEquals(1, tokenProvider.refreshCallCount.get())
        assertEquals(1, backendChannel.attempts.size)
    }

    @Test
    fun `concurrent UNAUTHENTICATED calls coalesce into a single refreshIfNeeded via Mutex`() = runTest {
        val tokenProvider = FakeRefreshingTokenProvider(initialToken = "expired-jwt") {
            delay(25.milliseconds)
            Result.success("shared-refreshed-jwt")
        }
        val backendChannel = ScriptedChannel { attempt ->
            if (attempt.authHeader == "Bearer shared-refreshed-jwt") {
                Status.OK to "ok:${attempt.requestPayload}"
            } else {
                Status.UNAUTHENTICATED.withDescription("Token expired") to null
            }
        }
        val interceptor = UnifiedGrpcClientInterceptor(
            tokenProvider = tokenProvider,
            dispatchers = TestCoroutineDispatchers(UnconfinedTestDispatcher(testScheduler)),
        )
        val clientChannel = intercept(backendChannel, interceptor)

        val responses = (1..5).map { idx ->
            async {
                clientChannel.unaryCall(addEntryMethod, "req-$idx")
            }
        }.awaitAll()

        assertEquals(listOf("ok:req-1", "ok:req-2", "ok:req-3", "ok:req-4", "ok:req-5"), responses)
        assertEquals(1, tokenProvider.refreshCallCount.get())
        assertEquals(10, backendChannel.attempts.size)
    }
}
