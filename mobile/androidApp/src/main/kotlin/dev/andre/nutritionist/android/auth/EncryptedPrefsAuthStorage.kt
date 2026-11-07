package dev.andre.nutritionist.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация безопасного хранилища AuthState на базе EncryptedSharedPreferences и Android Keystore.
 * Криптографические операции и чтение/запись выполняются строго на [dispatchers.io].
 */
class EncryptedPrefsAuthStorage(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val prefFileName: String = DEFAULT_PREF_NAME,
) : AuthStorage {

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            prefFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun getAuthStateJson(): String? = withContext(dispatchers.io) {
        sharedPreferences.getString(KEY_AUTH_STATE_JSON, null)
    }

    override suspend fun saveAuthStateJson(json: String): Unit = withContext(dispatchers.io) {
        sharedPreferences.edit()
            .putString(KEY_AUTH_STATE_JSON, json)
            .apply()
    }

    override suspend fun clear(): Unit = withContext(dispatchers.io) {
        sharedPreferences.edit()
            .remove(KEY_AUTH_STATE_JSON)
            .apply()
    }

    companion object {
        const val DEFAULT_PREF_NAME: String = "auth_encrypted_prefs"
        const val KEY_AUTH_STATE_JSON: String = "key_auth_state_json"
    }
}
