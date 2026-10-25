package dev.andre.nutritionist.data.remote.grpc

import dev.andre.nutritionist.data.remote.grpc.interceptor.AuthClientInterceptor
import dev.andre.nutritionist.data.remote.grpc.interceptor.TraceparentClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

object GrpcClientFactory {

    fun createManagedChannel(
        host: String,
        port: Int,
        authInterceptor: AuthClientInterceptor,
        traceInterceptor: TraceparentClientInterceptor,
    ): ManagedChannel {
        return ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .intercept(authInterceptor, traceInterceptor)
            .build()
    }
}
