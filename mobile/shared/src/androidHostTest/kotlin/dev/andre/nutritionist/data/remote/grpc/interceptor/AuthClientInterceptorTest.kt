package dev.andre.nutritionist.data.remote.grpc.interceptor

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthClientInterceptorTest {

    private val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

    private class FakeChannel : Channel() {
        var capturedHeaders: Metadata? = null

        override fun <RequestT, ResponseT> newCall(
            methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
            callOptions: CallOptions,
        ): ClientCall<RequestT, ResponseT> {
            return object : ClientCall<RequestT, ResponseT>() {
                override fun start(responseListener: Listener<ResponseT>, headers: Metadata) {
                    capturedHeaders = headers
                }
                override fun request(numMessages: Int) {}
                override fun cancel(message: String?, cause: Throwable?) {}
                override fun halfClose() {}
                override fun sendMessage(message: RequestT) {}
            }
        }

        override fun authority(): String = "localhost"
    }

    private class FakeAuthTokenProvider(private val token: String?) : AuthTokenProvider {
        override fun getAccessToken(): String? = token
        override fun isAuthenticated(): Boolean = token != null
        override suspend fun refreshIfNeeded(): Result<String> =
            token?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No token"))
    }

    private fun dummyMethod(): MethodDescriptor<String, String> {
        val marshaller = object : MethodDescriptor.Marshaller<String> {
            override fun stream(value: String) = value.byteInputStream()
            override fun parse(stream: java.io.InputStream) = stream.bufferedReader().readText()
        }
        return MethodDescriptor.newBuilder<String, String>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("test/TestMethod")
            .setRequestMarshaller(marshaller)
            .setResponseMarshaller(marshaller)
            .build()
    }

    @Test
    fun `interceptCall attaches authorization header when token is present`() {
        val channel = FakeChannel()
        val interceptor = AuthClientInterceptor(FakeAuthTokenProvider("jwt-token-123"))
        val call = interceptor.interceptCall(dummyMethod(), CallOptions.DEFAULT, channel)

        val headers = Metadata()
        call.start(object : ClientCall.Listener<String>() {}, headers)

        val captured = channel.capturedHeaders?.get(authKey)
        assertEquals("Bearer jwt-token-123", captured)
    }

    @Test
    fun `interceptCall does not attach authorization header when token is null`() {
        val channel = FakeChannel()
        val interceptor = AuthClientInterceptor(FakeAuthTokenProvider(null))
        val call = interceptor.interceptCall(dummyMethod(), CallOptions.DEFAULT, channel)

        val headers = Metadata()
        call.start(object : ClientCall.Listener<String>() {}, headers)

        val captured = channel.capturedHeaders?.get(authKey)
        assertNull(captured)
    }

    @Test
    fun `interceptCall does not attach authorization header when token is blank`() {
        val channel = FakeChannel()
        val interceptor = AuthClientInterceptor(FakeAuthTokenProvider("   "))
        val call = interceptor.interceptCall(dummyMethod(), CallOptions.DEFAULT, channel)

        val headers = Metadata()
        call.start(object : ClientCall.Listener<String>() {}, headers)

        val captured = channel.capturedHeaders?.get(authKey)
        assertNull(captured)
    }
}
