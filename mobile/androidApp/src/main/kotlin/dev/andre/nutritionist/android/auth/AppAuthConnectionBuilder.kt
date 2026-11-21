package dev.andre.nutritionist.android.auth

import android.net.Uri
import net.openid.appauth.connectivity.ConnectionBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Реализация [ConnectionBuilder] для AppAuth, поддерживающая как HTTPS (в продакшене),
 * так и HTTP для локальной разработки и тестирования (10.0.2.2 / localhost / 127.0.0.1).
 */
object AppAuthConnectionBuilder : ConnectionBuilder {
    private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
    private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()

    @Throws(IOException::class)
    override fun openConnection(uri: Uri): HttpURLConnection {
        val targetUri = resolveTargetUri(uri)
        val conn = URL(targetUri.toString()).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECTION_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = false
        return conn
    }

    private fun resolveTargetUri(uri: Uri): Uri {
        val host = uri.host
        val port = if (uri.port != -1) uri.port else 8080
        if (host == "10.0.2.2" && isPortReachable("127.0.0.1", port)) {
            return uri.buildUpon().encodedAuthority("127.0.0.1:$port").build()
        }
        return uri
    }

    private fun isPortReachable(host: String, port: Int, timeoutMs: Int = 300): Boolean =
        runCatching {
            val oldPolicy = android.os.StrictMode.getThreadPolicy()
            android.os.StrictMode.setThreadPolicy(android.os.StrictMode.ThreadPolicy.LAX)
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } finally {
                android.os.StrictMode.setThreadPolicy(oldPolicy)
            }
        }.getOrDefault(false)
}
