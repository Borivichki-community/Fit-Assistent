package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.local.dao.FavoriteProductDao
import dev.andre.nutritionist.data.local.entity.FavoriteProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFavoriteProductDao : FavoriteProductDao {

    // mutable by design: test control
    val items = linkedMapOf<String, FavoriteProductEntity>()
    private val itemsFlow = MutableStateFlow<List<FavoriteProductEntity>>(emptyList())
    var shouldThrow: Throwable? = null

    override suspend fun upsert(entity: FavoriteProductEntity) {
        shouldThrow?.let { throw it }
        items[entity.id] = entity
        emitSorted()
    }

    override suspend fun deleteById(id: String) {
        shouldThrow?.let { throw it }
        items.remove(id)
        emitSorted()
    }

    override fun observeAll(): Flow<List<FavoriteProductEntity>> = itemsFlow

    override fun isFavorite(id: String): Flow<Boolean> =
        itemsFlow.map { list -> list.any { it.id == id } }

    override suspend fun existsById(id: String): Boolean {
        shouldThrow?.let { throw it }
        return items.containsKey(id)
    }

    private fun emitSorted() {
        itemsFlow.value = items.values.sortedByDescending { it.savedAt }
    }

    fun reset() {
        items.clear()
        itemsFlow.value = emptyList()
        shouldThrow = null
    }
}
