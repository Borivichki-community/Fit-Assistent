package dev.andre.nutritionist.data.remote.grpc.interceptor

import dev.andre.nutritionist.domain.trace.TraceContextHolder
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TraceparentClientInterceptorTest {

    private val traceKey = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)

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

    private class FakeTraceContextHolder(private val trace: String?) : TraceContextHolder {
        override fun getTraceparent(): String? = trace
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
    fun `interceptCall attaches existing traceparent when available`() {
        val existingTrace = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        val channel = FakeChannel()
        val interceptor = TraceparentClientInterceptor(FakeTraceContextHolder(existingTrace))
        val call = interceptor.interceptCall(dummyMethod(), CallOptions.DEFAULT, channel)

        val headers = Metadata()
        call.start(object : ClientCall.Listener<String>() {}, headers)

        val captured = channel.capturedHeaders?.get(traceKey)
        assertEquals(existingTrace, captured)
    }

    @Test
    fun `interceptCall generates valid W3C traceparent when none exists`() {
        val channel = FakeChannel()
        val interceptor = TraceparentClientInterceptor(FakeTraceContextHolder(null))
        val call = interceptor.interceptCall(dummyMethod(), CallOptions.DEFAULT, channel)

        val headers = Metadata()
        call.start(object : ClientCall.Listener<String>() {}, headers)

        val captured = channel.capturedHeaders?.get(traceKey)
        assertNotNull(captured)
        // W3C traceparent format: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
        // Length must be exactly 55 characters
        assertEquals(55, captured.length)
        assertTrue(captured.startsWith("00-"))
        assertTrue(captured.endsWith("-01"))
        val parts = captured.split("-")
        assertEquals(4, parts.size)
        assertEquals("00", parts[0])
        assertEquals(32, parts[1].length) // 16 bytes hex trace_id
        assertEquals(16, parts[2].length) // 8 bytes hex span_id
        assertEquals("01", parts[3])      // flags
    }
}
