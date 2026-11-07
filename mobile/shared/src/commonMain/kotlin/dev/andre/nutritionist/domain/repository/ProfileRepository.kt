package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий профиля пользователя.
 * Реализует паттерн Single Source of Truth (SSOT): Room является единственным источником
 * данных для UI, сеть обновляет локальную базу.
 */
interface ProfileRepository {

    /**
     * Непрерывный реактивный поток данных профиля из локальной БД.
     * Эмитит null, если профиль для данного пользователя ещё не сохранён в БД.
     */
    fun observeProfile(userId: UserId): Flow<Profile?>

    /**
     * Однократное получение профиля: сначала из локального кэша Room,
     * а при его отсутствии — fallback-запрос в сеть с сохранением в БД.
     */
    suspend fun getProfile(userId: UserId): Profile?

    /**
     * Принудительное фоновое обновление данных профиля из сети.
     * При успешном ответе сохраняет запись в локальную БД, что автоматически
     * триггерит эмиссию в [observeProfile].
     */
    suspend fun refreshProfile(userId: UserId): Result<Unit>

    /**
     * Обновление данных профиля: выполняет мутацию на сервере и при успехе
     * немедленно актуализирует локальный кэш Room.
     */
    suspend fun updateProfile(profile: Profile): Result<Profile>
}