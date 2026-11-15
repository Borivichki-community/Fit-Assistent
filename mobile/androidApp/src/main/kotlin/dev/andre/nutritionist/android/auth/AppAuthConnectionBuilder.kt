package dev.andre.nutritionist.android.auth

import android.net.Uri
import net.openid.appauth.connectivity.ConnectionBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Реализация [ConnectionBuilder] для AppAuth, поддерживающая как HTTPS (в продакшене),
 * так и HTTP для локальной разработки и тестирования (10.0.2.2 / localhost).
 */
object AppAuthConnectionBuilder : ConnectionBuilder {
    private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
    private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()

    @Throws(IOException::class)
    override fun openConnection(uri: Uri): HttpURLConnection {
        val conn = URL(uri.toString()).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECTION_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = false
        return conn
    }
}
