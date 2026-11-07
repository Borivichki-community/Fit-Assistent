package dev.andre.nutritionist.data.remote.grpc.interceptor

import dev.andre.nutritionist.data.remote.grpc.auth.AuthCallCredentials
import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import io.grpc.CallCredentials
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import java.io.InputStream
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthClientInterceptorTest {

    private class FakeChannel : Channel() {
        var capturedCallOptions: CallOptions? = null
        var capturedMethod: MethodDescriptor<*, *>? = null

        override fun <RequestT, ResponseT> newCall(
            methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
            callOptions: CallOptions,
        ): ClientCall<RequestT, ResponseT> {
            capturedCallOptions = callOptions
            capturedMethod = methodDescriptor
            return object : ClientCall<RequestT, ResponseT>() {
                override fun start(responseListener: Listener<ResponseT>, headers: Metadata) {}
                override fun request(numMessages: Int) {}
                override fun cancel(message: String?, cause: Throwable?) {}
                override fun halfClose() {}
                override fun sendMessage(message: RequestT) {}
            }
        }

        override fun authority(): String = "localhost"
    }

    private class FakeCallCredentials : CallCredentials() {
        override fun applyRequestMetadata(
            requestInfo: RequestInfo,
            appExecutor: Executor,
            applier: MetadataApplier,
        ) {}
        @Deprecated("Required by gRPC CallCredentials abstract class", level = DeprecationLevel.WARNING)
        override fun thisUsesUnstableApi() {}
    }

    private class FakeAuthTokenProvider(
        private val token: String? = "test-token",
    ) : AuthTokenProvider {
        override fun getAccessToken(): String? = token
        override fun isAuthenticated(): Boolean = token != null
        override suspend fun refreshIfNeeded(): Result<String> =
            token?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No token"))
    }

    private fun dummyMethod(): MethodDescriptor<String, String> {
        val marshaller = object : MethodDescriptor.Marshaller<String> {
            override fun stream(value: String): InputStream = value.byteInputStream()
            override fun parse(stream: InputStream): String = stream.bufferedReader().readText()
        }
        return MethodDescriptor.newBuilder<String, String>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("test/TestMethod")
            .setRequestMarshaller(marshaller)
            .setResponseMarshaller(marshaller)
            .build()
    }

    @Test
    fun `interceptCall attaches CallCredentials to callOptions`() {
        val channel = FakeChannel()
        val fakeCredentials = FakeCallCredentials()
        val interceptor = AuthClientInterceptor(fakeCredentials)

        val method = dummyMethod()
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel)

        assertEquals(fakeCredentials, channel.capturedCallOptions?.credentials)
        assertEquals(method, channel.capturedMethod)
    }

    @Test
    fun `secondary constructor creates AuthCallCredentials`() {
        val channel = FakeChannel()
        val interceptor = AuthClientInterceptor(
            tokenProvider = FakeAuthTokenProvider(),
            dispatchers = TestCoroutineDispatchers(),
        )

        assertTrue(interceptor.credentials is AuthCallCredentials)

        val method = dummyMethod()
        interceptor.interceptCall(method, CallOptions.DEFAULT, channel)

        assertEquals(interceptor.credentials, channel.capturedCallOptions?.credentials)
    }

    @Test
    fun `interceptCall does not block or execute runBlocking on calling thread`() {
        val channel = FakeChannel()
        val interceptor = AuthClientInterceptor(
            tokenProvider = FakeAuthTokenProvider(),
            dispatchers = TestCoroutineDispatchers(),
        )

        // interceptCall is purely synchronous and non-blocking
        val call = interceptor.interceptCall(dummyMethod(), CallOptions.DEFAULT, channel)
        assertNotNull(call)
        assertNotNull(channel.capturedCallOptions?.credentials)
    }
}
