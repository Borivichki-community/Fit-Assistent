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
        val (host, port) = resolveGrpcTarget()
        android.util.Log.i("FitGrpc", "Targeting gRPC service at host=$host, port=$port")
        GrpcClientFactory.createManagedChannel(
            host = host,
            port = port,
            authInterceptor = get(),
            traceInterceptor = get(),
        )
    }
    single<ProfileGrpcServiceClient> { AndroidProfileGrpcServiceClient(channel = get()) }
    single { ProfileGrpcRemoteDataSource(client = get(), dispatchers = get()) } bind ProfileRemoteDataSource::class
}

private fun resolveGrpcTarget(): Pair<String, Int> {
    if (isPortReachable("127.0.0.1", 9090)) return "127.0.0.1" to 9090
    if (isPortReachable("10.0.2.2", 9090)) return "10.0.2.2" to 9090
    if (isPortReachable("127.0.0.1", 9091)) return "127.0.0.1" to 9091
    if (isPortReachable("10.0.2.2", 9091)) return "10.0.2.2" to 9091
    return if (isEmulator()) "127.0.0.1" to 9091 else "10.0.2.2" to 9091
}

private fun isEmulator(): Boolean {
    val fp = android.os.Build.FINGERPRINT
    val model = android.os.Build.MODEL
    val hw = android.os.Build.HARDWARE
    return fp.startsWith("generic") || fp.startsWith("unknown") ||
        model.contains("google_sdk") || model.contains("Emulator") ||
        hw.contains("goldfish") || hw.contains("ranchu")
}

private fun isPortReachable(host: String, port: Int, timeoutMs: Int = 300): Boolean =
    runCatching {
        val oldPolicy = android.os.StrictMode.getThreadPolicy()
        android.os.StrictMode.setThreadPolicy(android.os.StrictMode.ThreadPolicy.LAX)
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
                true
            }
        } finally {
            android.os.StrictMode.setThreadPolicy(oldPolicy)
        }
    }.getOrDefault(false)

val androidLocalDataModule = module {
    single<dev.andre.nutritionist.data.local.DatabaseBuilder<dev.andre.nutritionist.data.local.AppDatabase>> {
        dev.andre.nutritionist.data.local.AndroidDatabaseBuilder(context = get())
    }
}

