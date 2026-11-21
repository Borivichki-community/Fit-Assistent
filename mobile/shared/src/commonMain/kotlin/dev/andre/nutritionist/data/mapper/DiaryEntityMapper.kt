package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.DiaryEntryEntity
import dev.andre.nutritionist.domain.model.DiaryEntry
import dev.andre.nutritionist.domain.model.DiaryEntryId
import dev.andre.nutritionist.domain.model.FoodItemId
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.SyncStatus
import dev.andre.nutritionist.domain.model.UserId
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

fun DiaryEntry.toEntity(syncStatusOverride: String? = null): DiaryEntryEntity = DiaryEntryEntity(
    id = id.raw,
    userId = userId.raw,
    foodItemId = foodItemId.raw,
    foodName = foodName,
    portionGrams = portionGrams,
    calories = calories,
    proteins = proteins,
    fats = fats,
    carbs = carbs,
    mealType = mealType.name,
    dateEpochDay = date.toEpochDays().toLong(),
    recordedAtEpochMs = recordedAt.toEpochMilliseconds(),
    idempotencyKey = idempotencyKey.raw,
    portionDescription = portionDescription,
    syncStatus = syncStatusOverride ?: when (syncStatus) {
        SyncStatus.PENDING -> SyncStatus.STATUS_SYNC_PENDING
        SyncStatus.SYNCED -> SyncStatus.STATUS_SYNCED
        SyncStatus.FAILED -> SyncStatus.STATUS_FAILED
    },
)

fun DiaryEntryEntity.toDomain(): DiaryEntry {
    val domainMealType = MealType.entries.firstOrNull { it.name.equals(mealType, ignoreCase = true) }
        ?: MealType.SNACK

    val domainSyncStatus = when (syncStatus.uppercase()) {
        SyncStatus.STATUS_SYNCED -> SyncStatus.SYNCED
        SyncStatus.STATUS_FAILED -> SyncStatus.FAILED
        else -> SyncStatus.PENDING
    }

    return DiaryEntry(
        id = DiaryEntryId(id),
        userId = UserId(userId),
        foodItemId = FoodItemId(foodItemId),
        foodName = foodName,
        portionGrams = portionGrams,
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbs = carbs,
        mealType = domainMealType,
        date = LocalDate.fromEpochDays(dateEpochDay.toInt()),
        recordedAt = Instant.fromEpochMilliseconds(recordedAtEpochMs),
        idempotencyKey = IdempotencyKey(idempotencyKey),
        portionDescription = portionDescription,
        syncStatus = domainSyncStatus,
    )
}
