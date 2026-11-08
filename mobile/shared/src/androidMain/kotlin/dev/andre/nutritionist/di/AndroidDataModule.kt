package dev.andre.nutritionist.di

import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.AndroidProfileGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.GrpcClientFactory
import dev.andre.nutritionist.data.remote.grpc.ProfileGrpcRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.ProfileGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.auth.AuthCallCredentials
import dev.andre.nutritionist.data.remote.grpc.interceptor.AuthClientInterceptor
import dev.andre.nutritionist.data.remote.grpc.interceptor.TraceparentClientInterceptor
import dev.andre.nutritionist.domain.trace.DefaultTraceContextHolder
import dev.andre.nutritionist.domain.trace.TraceContextHolder
import org.koin.dsl.bind
import org.koin.dsl.module

val androidRemoteDataModule = module {
    single<TraceContextHolder> { DefaultTraceContextHolder() }
    single { AuthCallCredentials(tokenProvider = get(), dispatchers = get()) }
    single { AuthClientInterceptor(credentials = get<AuthCallCredentials>()) }
    single { TraceparentClientInterceptor(traceContextHolder = get()) }
    single {
        GrpcClientFactory.createManagedChannel(
            host = "10.0.2.2",
            port = 9090,
            authInterceptor = get(),
            traceInterceptor = get(),
        )
    }
    single<ProfileGrpcServiceClient> { AndroidProfileGrpcServiceClient(channel = get()) }
    single { ProfileGrpcRemoteDataSource(client = get(), dispatchers = get()) } bind ProfileRemoteDataSource::class
}

val androidLocalDataModule = module {
    single<dev.andre.nutritionist.data.local.DatabaseBuilder<dev.andre.nutritionist.data.local.AppDatabase>> {
        dev.andre.nutritionist.data.local.AndroidDatabaseBuilder(context = get())
    }
}

