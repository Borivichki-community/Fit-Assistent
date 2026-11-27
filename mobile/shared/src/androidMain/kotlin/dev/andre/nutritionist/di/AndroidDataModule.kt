package dev.andre.nutritionist.di

import dev.andre.nutritionist.data.remote.CatalogRemoteDataSource
import dev.andre.nutritionist.data.remote.DiaryRemoteDataSource
import dev.andre.nutritionist.data.remote.ProfileRemoteDataSource
import dev.andre.nutritionist.data.remote.RecipeRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.AndroidCatalogGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.AndroidDiaryGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.AndroidProfileGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.AndroidRecipeGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.CatalogGrpcRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.CatalogGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.DiaryGrpcRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.DiaryGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.GrpcClientFactory
import dev.andre.nutritionist.data.remote.grpc.GrpcErrorMapper
import dev.andre.nutritionist.data.remote.grpc.ProfileGrpcRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.ProfileGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.RecipeGrpcRemoteDataSource
import dev.andre.nutritionist.data.remote.grpc.RecipeGrpcServiceClient
import dev.andre.nutritionist.data.remote.grpc.auth.AuthCallCredentials
import dev.andre.nutritionist.data.remote.grpc.interceptor.AuthClientInterceptor
import dev.andre.nutritionist.data.remote.grpc.interceptor.TraceparentClientInterceptor
import dev.andre.nutritionist.data.remote.grpc.interceptors.UnifiedGrpcClientInterceptor
import dev.andre.nutritionist.domain.network.ConnectivityObserver
import dev.andre.nutritionist.domain.trace.DefaultTraceContextHolder
import dev.andre.nutritionist.domain.trace.TraceContextHolder
import dev.andre.nutritionist.network.AndroidConnectivityObserver
import org.koin.dsl.bind
import org.koin.dsl.module

val androidRemoteDataModule = module {
    single<TraceContextHolder> { DefaultTraceContextHolder() }
    single { AuthCallCredentials(tokenProvider = get(), dispatchers = get()) }
    single { AuthClientInterceptor(credentials = get<AuthCallCredentials>()) }
    single { TraceparentClientInterceptor(traceContextHolder = get()) }
    single {
        UnifiedGrpcClientInterceptor(
            tokenProvider = get(),
            traceContextHolder = get(),
            dispatchers = get(),
        )
    }
    single<ConnectivityObserver> { AndroidConnectivityObserver(context = get()) }
    single {
        GrpcClientFactory.createManagedChannel(
            host = "10.0.2.2",
            port = 9090,
            unifiedInterceptor = get(),
        )
    } bind io.grpc.Channel::class
    single<ProfileGrpcServiceClient> { AndroidProfileGrpcServiceClient(channel = get()) }
    single { ProfileGrpcRemoteDataSource(client = get(), dispatchers = get()) } bind ProfileRemoteDataSource::class

    single<CatalogGrpcServiceClient> { AndroidCatalogGrpcServiceClient(channel = get()) }
    single { CatalogGrpcRemoteDataSource(client = get(), dispatchers = get()) } bind CatalogRemoteDataSource::class

    single<DiaryGrpcServiceClient> { AndroidDiaryGrpcServiceClient(channel = get()) }
    single {
        DiaryGrpcRemoteDataSource(
            client = get(),
            dispatchers = get(),
            errorMapper = { GrpcErrorMapper.map(it) },
        )
    } bind DiaryRemoteDataSource::class

    single<RecipeGrpcServiceClient> { AndroidRecipeGrpcServiceClient(channel = get()) }
    single {
        RecipeGrpcRemoteDataSource(
            client = get(),
            dispatchers = get(),
            errorMapper = { GrpcErrorMapper.map(it) },
        )
    } bind RecipeRemoteDataSource::class
}

val androidLocalDataModule = module {
    single<dev.andre.nutritionist.data.local.DatabaseBuilder<dev.andre.nutritionist.data.local.AppDatabase>> {
        dev.andre.nutritionist.data.local.AndroidDatabaseBuilder(context = get())
    }
}

