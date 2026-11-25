package dev.andre.nutritionist.data.mapper

import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.IdempotencyKey
import dev.andre.nutritionist.domain.model.MealRecord
import dev.andre.nutritionist.domain.model.MealRecordId
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.Money
import dev.andre.nutritionist.domain.model.NutritionFacts
import dev.andre.nutritionist.domain.model.SyncStatus
import kotlinx.datetime.Instant

object MealRecordEntityMapper {

    fun MealRecordEntity.toDomain(): MealRecord = MealRecord(
        id = MealRecordId(id),
        mealType = when (mealType.uppercase()) {
            "BREAKFAST" -> MealType.BREAKFAST
            "LUNCH" -> MealType.LUNCH
            "DINNER" -> MealType.DINNER
            "SNACK" -> MealType.SNACK
            else -> MealType.SNACK
        },
        recordedAt = Instant.fromEpochMilliseconds(recordedAtEpoch),
        sourceType = when (sourceType.uppercase()) {
            "PRODUCT" -> EntrySourceType.PRODUCT
            "RECIPE" -> EntrySourceType.RECIPE
            "CUSTOM" -> EntrySourceType.CUSTOM
            else -> EntrySourceType.UNSPECIFIED
        },
        sourceId = sourceId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        nutrition = NutritionFacts(
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
        ),
        cost = costAmountMinor?.let { Money.fromMinor(it) },
        idempotencyKey = IdempotencyKey(idempotencyKey),
        syncStatus = when {
            isDeleted || syncStatus.uppercase() == "DELETED_PENDING" -> SyncStatus.DELETED_PENDING
            syncStatus.uppercase() == "SYNCED" -> SyncStatus.SYNCED
            syncStatus.uppercase() == "FAILED" -> SyncStatus.FAILED
            else -> SyncStatus.PENDING
        },
    )

    fun MealRecord.toEntity(): MealRecordEntity = MealRecordEntity(
        id = id.raw,
        idempotencyKey = idempotencyKey.raw,
        syncStatus = syncStatus.name,
        mealType = mealType.name,
        recordedAtEpoch = recordedAt.toEpochMilliseconds(),
        sourceType = sourceType.name,
        sourceId = sourceId,
        nameSnapshot = nameSnapshot,
        weightGrams = weightGrams,
        calories = nutrition.calories,
        proteins = nutrition.proteins,
        fats = nutrition.fats,
        carbs = nutrition.carbs,
        costAmountMinor = cost?.amountMinor,
        costCurrency = if (cost != null) Money.CURRENCY_RUB else null,
        isDeleted = syncStatus == SyncStatus.DELETED_PENDING,
    )
}
