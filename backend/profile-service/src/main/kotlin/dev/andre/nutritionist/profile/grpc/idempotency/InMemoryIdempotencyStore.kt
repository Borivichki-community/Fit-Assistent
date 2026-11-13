package dev.andre.nutritionist.profile.grpc.idempotency

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Потокобезопасная реализация хранилища идемпотентности в оперативной памяти.
 */
@Component
class InMemoryIdempotencyStore : IdempotencyStore {
    private val store = ConcurrentHashMap<String, ByteArray>()

    override fun get(key: String): ByteArray? = store[key]

    override fun put(
        key: String,
        responseBytes: ByteArray,
    ) {
        store[key] = responseBytes
    }

    fun clear() {
        store.clear()
    }
}
