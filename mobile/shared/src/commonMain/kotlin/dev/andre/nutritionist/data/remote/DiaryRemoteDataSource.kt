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
 */
class DefaultDiaryRemoteDataSource(
    private val dispatchers: CoroutineDispatchers,
) : DiaryRemoteDataSource {

    override suspend fun syncEntry(entry: DiaryEntry): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                // Идемпотентная отправка на сервер
            }
        }
}
