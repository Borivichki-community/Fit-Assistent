package dev.andre.nutritionist.di

import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.GrpcClientFactory
import dev.andre.nutritionist.data.remote.grpc.ProfileGrpcRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.interceptor.AuthClientInterceptor
import dev.andre.nutritionist.data.remote.grpc.interceptor.TraceparentClientInterceptor
import org.koin.dsl.bind
import org.koin.dsl.module

val androidRemoteDataModule = module {
    single { AuthClientInterceptor(tokenProvider = get()) }
    single { TraceparentClientInterceptor(traceContextHolder = get()) }
    single {
        GrpcClientFactory.createManagedChannel(
            host = "10.0.2.2",
            port = 9090,
            authInterceptor = get(),
            traceInterceptor = get(),
        )
    }
    single { ProfileGrpcRemoteDataSource(client = get(), dispatchers = get()) } bind ProfileRemoteDataSource::class
}
