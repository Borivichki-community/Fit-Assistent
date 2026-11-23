package dev.andre.nutritionist.data.remote

import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.DiaryEntry
import kotlinx.coroutines.withContext

/**
 * Контракт удалённого источника данных дневника (gRPC).
 */
interface DiaryRemoteDataSource {
    suspend fun syncEntry(entry: DiaryEntry): Result<Unit>
}

/**
 * Реализация удалённого источника данных по умолчанию.
 * Поддерживает установку внешнего делегата синхронизации для платформенного gRPC-клиента.
 */
class DefaultDiaryRemoteDataSource(
    private val dispatchers: CoroutineDispatchers,
    private val remoteSyncHandler: (suspend (DiaryEntry) -> Result<Unit>)? = null,
) : DiaryRemoteDataSource {

    override suspend fun syncEntry(entry: DiaryEntry): Result<Unit> =
        withContext(dispatchers.io) {
            val handler = remoteSyncHandler ?: companionSyncHandler
            if (handler != null) {
                handler(entry)
            } else {
                Result.success(Unit)
            }
        }

    companion object {
        var companionSyncHandler: (suspend (DiaryEntry) -> Result<Unit>)? = null
    }
}
