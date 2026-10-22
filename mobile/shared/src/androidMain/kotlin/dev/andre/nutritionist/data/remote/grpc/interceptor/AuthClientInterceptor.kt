package dev.andre.nutritionist.data.remote.grpc.interceptor

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor


class AuthClientInterceptor(
    private val tokenProvider: AuthTokenProvider,
) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val token = tokenProvider.getAccessToken()
                if (!token.isNullOrBlank()) {
                    headers.put(AUTHORIZATION_METADATA_KEY, "Bearer $token")
                }
                super.start(responseListener, headers)
            }
        }
    }

    companion object {
        private val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    }
}
