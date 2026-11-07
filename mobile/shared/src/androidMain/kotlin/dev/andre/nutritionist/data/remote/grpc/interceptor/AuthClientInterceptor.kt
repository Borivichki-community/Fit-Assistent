package dev.andre.nutritionist.data.remote.grpc.interceptor

import dev.andre.nutritionist.data.remote.grpc.auth.AuthCallCredentials
import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import io.grpc.CallCredentials
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.Metadata
import io.grpc.MethodDescriptor


class AuthClientInterceptor(
    val credentials: CallCredentials,
) : ClientInterceptor {

    constructor(
        tokenProvider: AuthTokenProvider,
        dispatchers: CoroutineDispatchers,
    ) : this(AuthCallCredentials(tokenProvider, dispatchers))

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        return next.newCall(method, callOptions.withCallCredentials(credentials))
    }

    companion object {
        val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
            AuthCallCredentials.AUTHORIZATION_METADATA_KEY
    }
}
