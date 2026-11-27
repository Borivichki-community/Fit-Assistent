package dev.andre.nutritionist.domain.auth

import dev.andre.nutritionist.domain.model.UserId

/**
 * Поставщик идентификатора текущего авторизованного пользователя для слоя :shared.
 */
fun interface UserIdProvider {
    fun getUserId(): UserId?
}
