package dev.andre.nutritionist.presentation.profile

import dev.andre.nutritionist.domain.model.Profile
import dev.andre.nutritionist.domain.model.UserId

sealed interface ProfileCommand {
    /** Команда загрузки и реактивного наблюдения профиля (SSOT из Room + фоновый refresh). */
    data class LoadProfile(val userId: UserId) : ProfileCommand

    /** Принудительное обновление профиля из удалённого источника (pull-to-refresh / retry). */
    data class RefreshProfile(val userId: UserId) : ProfileCommand

    /** Мутация/сохранение профиля на бэкенде с обновлением локального Room-кэша. */
    data class UpdateProfile(val profile: Profile) : ProfileCommand
}
