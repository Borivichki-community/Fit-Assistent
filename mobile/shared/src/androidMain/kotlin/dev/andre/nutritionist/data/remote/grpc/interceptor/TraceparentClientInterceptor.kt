package dev.andre.nutritionist.data.remote.grpc.interceptor

import dev.andre.nutritionist.domain.trace.TraceContextHolder
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import kotlin.random.Random


class TraceparentClientInterceptor(
    private val traceContextHolder: TraceContextHolder,
) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val traceparent = traceContextHolder.getTraceparent() ?: generateTraceparent()
                headers.put(TRACEPARENT_METADATA_KEY, traceparent)
                super.start(responseListener, headers)
            }
        }
    }

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
        private val TRACEPARENT_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)
    }
}
