package dev.andre.nutritionist.domain.model

import kotlinx.datetime.Instant

enum class EntrySourceType {
    UNSPECIFIED,
    PRODUCT,
    RECIPE,
    CUSTOM,
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED,
    DELETED_PENDING,
}

data class MealRecord(
    val id: MealRecordId,
    val mealType: MealType,
    val recordedAt: Instant,
    val sourceType: EntrySourceType,
    val sourceId: String? = null,
    val nameSnapshot: String,
    val weightGrams: Double,
    val nutrition: NutritionFacts,
    val cost: Money? = null,
    val idempotencyKey: IdempotencyKey = IdempotencyKey.generate(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
)
