package dev.andre.nutritionist.fake

import dev.andre.nutritionist.data.local.dao.SavedRecipeDao
import dev.andre.nutritionist.data.local.entity.SavedRecipeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSavedRecipeDao : SavedRecipeDao {

    // mutable by design: test control
    val items = linkedMapOf<String, SavedRecipeEntity>()
    private val itemsFlow = MutableStateFlow<List<SavedRecipeEntity>>(emptyList())
    var shouldThrow: Throwable? = null

    override suspend fun upsert(entity: SavedRecipeEntity) {
        shouldThrow?.let { throw it }
        items[entity.id] = entity
        emitSorted()
    }

    override suspend fun deleteById(id: String) {
        shouldThrow?.let { throw it }
        items.remove(id)
        emitSorted()
    }

    override fun observeAll(): Flow<List<SavedRecipeEntity>> = itemsFlow

    override fun isFavorite(id: String): Flow<Boolean> =
        itemsFlow.map { list -> list.any { it.id == id } }

    private fun emitSorted() {
        itemsFlow.value = items.values.sortedByDescending { it.savedAt }
    }

    fun reset() {
        items.clear()
        itemsFlow.value = emptyList()
        shouldThrow = null
    }
}
