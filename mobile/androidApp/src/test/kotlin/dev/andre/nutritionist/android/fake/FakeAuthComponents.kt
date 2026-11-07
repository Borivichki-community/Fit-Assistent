package dev.andre.nutritionist.android.fake

import dev.andre.nutritionist.android.auth.AuthStorage
import dev.andre.nutritionist.android.auth.TokenExchanger
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse

/**
 * Ручной Fake для безопасного хранилища [AuthStorage].
 * Хранит сериализованный AuthState в памяти, исключая зависимости от Android Keystore.
 */
class FakeAuthStorage(initialJson: String? = null) : AuthStorage {

    var savedJson: String? = initialJson
    var saveCallCount: Int = 0
    var clearCallCount: Int = 0

    override suspend fun getAuthStateJson(): String? = savedJson

    override suspend fun saveAuthStateJson(json: String) {
        savedJson = json
        saveCallCount++
    }

    override suspend fun clear() {
        savedJson = null
        clearCallCount++
    }
}

/**
 * Ручной Fake для [TokenExchanger].
 * Позволяет эмулировать успешный обмен authorization code на токены или сетевой сбой.
 */
class FakeTokenExchanger : TokenExchanger {

    var tokenResponseToReturn: TokenResponse? = null
    var exceptionToThrow: Exception? = null
    var exchangeCallCount: Int = 0

    override suspend fun exchange(request: TokenRequest): TokenResponse {
        exchangeCallCount++
        exceptionToThrow?.let { throw it }
        return requireNotNull(tokenResponseToReturn) {
            "FakeTokenExchanger: neither tokenResponseToReturn nor exceptionToThrow was set"
        }
    }
}
