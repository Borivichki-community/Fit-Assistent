package dev.andre.nutritionist.domain.model

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Реестр строго типизированных идентификаторов (Value Classes).
 * Запрещено использовать сырые String/Int в роли ID.
 */
@JvmInline
value class UserId(val raw: String)

@JvmInline
value class RecipeId(val raw: String)

@JvmInline
value class DiaryEntryId(val raw: String)

@JvmInline
value class FoodItemId(val raw: String)

@JvmInline
value class ProfileId(val raw: String)

@JvmInline
value class IdempotencyKey(val raw: String) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun generate(): IdempotencyKey = IdempotencyKey(Uuid.random().toString())
    }
}

@JvmInline
value class AccessToken(val raw: String)

@JvmInline
value class RefreshToken(val raw: String)

@JvmInline
value class IdToken(val raw: String)
