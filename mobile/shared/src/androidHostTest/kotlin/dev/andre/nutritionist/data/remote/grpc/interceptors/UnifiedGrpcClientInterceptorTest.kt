package dev.andre.nutritionist.data.remote.grpc.interceptors

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.trace.TraceContextHolder
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class UnifiedGrpcClientInterceptorTest {

    private val fakeTokenProvider = object : AuthTokenProvider {
        var token: String? = "test-jwt-token"
        override fun getAccessToken(): String? = token
        override fun isAuthenticated(): Boolean = token != null
        override suspend fun refreshIfNeeded(): Result<String> = Result.success(token ?: "")
    }

    private val fakeTraceHolder = object : TraceContextHolder {
        var currentTraceparent: String? = null
        override fun getTraceparent(): String? = currentTraceparent
    }

    private val interceptor = UnifiedGrpcClientInterceptor(
        tokenProvider = fakeTokenProvider,
        traceContextHolder = fakeTraceHolder,
    )

    private val testMarshaller = object : MethodDescriptor.Marshaller<String> {
        override fun stream(value: String): InputStream = ByteArrayInputStream(value.toByteArray())
        override fun parse(stream: InputStream): String = String(stream.readBytes())
    }

    private fun createMethod(fullMethodName: String): MethodDescriptor<String, String> =
        MethodDescriptor.newBuilder<String, String>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(fullMethodName)
            .setRequestMarshaller(testMarshaller)
            .setResponseMarshaller(testMarshaller)
            .build()

    @Test
    fun `interceptCall injects authorization header with Bearer`() {
        val method = createMethod("com.budgetntr.test.TestService/GetData")
        val capturedHeaders = Metadata()

        val mockCall = object : ClientCall<String, String>() {
            override fun start(responseListener: Listener<String>, headers: Metadata) {
                capturedHeaders.merge(headers)
            }
            override fun request(numMessages: Int) {}
            override fun cancel(message: String?, cause: Throwable?) {}
            override fun halfClose() {}
            override fun sendMessage(message: String) {}
        }

        val fakeChannel = object : Channel() {
            override fun <RequestT, ResponseT> newCall(
                methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
                callOptions: CallOptions,
            ): ClientCall<RequestT, ResponseT> {
                @Suppress("UNCHECKED_CAST")
                return mockCall as ClientCall<RequestT, ResponseT>
            }
            override fun authority(): String = "localhost"
        }

        val call = interceptor.interceptCall(method, CallOptions.DEFAULT, fakeChannel)
        call.start(object : ClientCall.Listener<String>() {}, Metadata())

        val auth = capturedHeaders.get(UnifiedGrpcClientInterceptor.AUTHORIZATION_METADATA_KEY)
        assertEquals("Bearer test-jwt-token", auth)
    }

    @Test
    fun `interceptCall injects traceparent header matching W3C format`() {
        fakeTraceHolder.currentTraceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

        val method = createMethod("com.budgetntr.test.TestService/GetData")
        val capturedHeaders = Metadata()

        val mockCall = object : ClientCall<String, String>() {
            override fun start(responseListener: Listener<String>, headers: Metadata) {
                capturedHeaders.merge(headers)
            }
            override fun request(numMessages: Int) {}
            override fun cancel(message: String?, cause: Throwable?) {}
            override fun halfClose() {}
            override fun sendMessage(message: String) {}
        }

        val fakeChannel = object : Channel() {
            override fun <RequestT, ResponseT> newCall(
                methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
                callOptions: CallOptions,
            ): ClientCall<RequestT, ResponseT> {
                @Suppress("UNCHECKED_CAST")
                return mockCall as ClientCall<RequestT, ResponseT>
            }
            override fun authority(): String = "localhost"
        }

        val call = interceptor.interceptCall(method, CallOptions.DEFAULT, fakeChannel)
        call.start(object : ClientCall.Listener<String>() {}, Metadata())

        val traceparent = capturedHeaders.get(UnifiedGrpcClientInterceptor.TRACEPARENT_METADATA_KEY)
        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", traceparent)
    }

    @Test
    fun `interceptCall injects idempotency-key header for mutating RPC methods`() {
        val mutatingMethod = createMethod("com.budgetntr.diary.v1.DiaryService/AddEntry")
        val capturedHeaders = Metadata()

        val mockCall = object : ClientCall<String, String>() {
            override fun start(responseListener: Listener<String>, headers: Metadata) {
                capturedHeaders.merge(headers)
            }
            override fun request(numMessages: Int) {}
            override fun cancel(message: String?, cause: Throwable?) {}
            override fun halfClose() {}
            override fun sendMessage(message: String) {}
        }

        val fakeChannel = object : Channel() {
            override fun <RequestT, ResponseT> newCall(
                methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
                callOptions: CallOptions,
            ): ClientCall<RequestT, ResponseT> {
                @Suppress("UNCHECKED_CAST")
                return mockCall as ClientCall<RequestT, ResponseT>
            }
            override fun authority(): String = "localhost"
        }

        val call = interceptor.interceptCall(mutatingMethod, CallOptions.DEFAULT, fakeChannel)
        call.start(object : ClientCall.Listener<String>() {}, Metadata())

        val idempotencyKey = capturedHeaders.get(UnifiedGrpcClientInterceptor.IDEMPOTENCY_METADATA_KEY)
        assertNotNull(idempotencyKey)
        assertTrue(idempotencyKey?.isNotEmpty() == true)
    }

    @Test
    fun `interceptCall respects idempotency key from Context`() {
        val method = createMethod("com.budgetntr.diary.v1.DiaryService/AddEntry")
        val capturedHeaders = Metadata()

        val mockCall = object : ClientCall<String, String>() {
            override fun start(responseListener: Listener<String>, headers: Metadata) {
                capturedHeaders.merge(headers)
            }
            override fun request(numMessages: Int) {}
            override fun cancel(message: String?, cause: Throwable?) {}
            override fun halfClose() {}
            override fun sendMessage(message: String) {}
        }

        val fakeChannel = object : Channel() {
            override fun <RequestT, ResponseT> newCall(
                methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
                callOptions: CallOptions,
            ): ClientCall<RequestT, ResponseT> {
                @Suppress("UNCHECKED_CAST")
                return mockCall as ClientCall<RequestT, ResponseT>
            }
            override fun authority(): String = "localhost"
        }

        val ctx = Context.current().withValue(UnifiedGrpcClientInterceptor.IDEMPOTENCY_KEY_CTX, "custom-idempotency-key-777")
        val prev = ctx.attach()
        try {
            val call = interceptor.interceptCall(method, CallOptions.DEFAULT, fakeChannel)
            call.start(object : ClientCall.Listener<String>() {}, Metadata())

            val key = capturedHeaders.get(UnifiedGrpcClientInterceptor.IDEMPOTENCY_METADATA_KEY)
            assertEquals("custom-idempotency-key-777", key)
        } finally {
            ctx.detach(prev)
        }
    }
}
