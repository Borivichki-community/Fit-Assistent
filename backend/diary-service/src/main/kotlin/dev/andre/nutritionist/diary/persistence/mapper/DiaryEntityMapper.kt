package dev.andre.nutritionist.diary.persistence.mapper

import dev.andre.nutritionist.diary.domain.model.AddEntryCommand
import dev.andre.nutritionist.diary.domain.model.CostSnapshot
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.Money
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import dev.andre.nutritionist.diary.persistence.entity.DiaryEntryEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Instant

@Component
class DiaryEntityMapper {
    fun toEntity(command: AddEntryCommand): DiaryEntryEntity =
        DiaryEntryEntity(
            id = command.entryId,
            userId = command.userId,
            idempotencyKey = command.idempotencyKey,
            foodItemId = command.foodItemId,
            foodName = command.foodName,
            quantityGrams = BigDecimal.valueOf(command.quantityGrams).setScale(3, RoundingMode.HALF_UP),
            calories = BigDecimal.valueOf(command.nutrition.calories.toLong()).setScale(2),
            proteins = BigDecimal.valueOf(command.nutrition.proteins).setScale(2, RoundingMode.HALF_UP),
            fats = BigDecimal.valueOf(command.nutrition.fats).setScale(2, RoundingMode.HALF_UP),
            carbs = BigDecimal.valueOf(command.nutrition.carbs).setScale(2, RoundingMode.HALF_UP),
            mealType = command.mealType,
            recordedAt =
                java.time.Instant.ofEpochSecond(
                    command.recordedAt.epochSeconds,
                    command.recordedAt.nanosecondsOfSecond.toLong(),
                ),
            price =
                command.cost
                    ?.total
                    ?.amountMinor
                    ?.let { BigDecimal.valueOf(it, 2) },
            unitPriceMinor = command.cost?.unitPrice?.amountMinor,
            priceBasisMilligrams = command.cost?.priceBasisMilligrams,
        )

    fun toDomain(entity: DiaryEntryEntity): DiaryEntry =
        DiaryEntry(
            id = entity.id,
            userId = entity.userId,
            foodItemId = entity.foodItemId,
            foodName = entity.foodName,
            quantityGrams = entity.quantityGrams.toDouble(),
            nutrition =
                NutritionSummary(
                    entity.calories.intValueExact(),
                    entity.proteins.toDouble(),
                    entity.fats.toDouble(),
                    entity.carbs.toDouble(),
                ),
            mealType = entity.mealType,
            recordedAt = Instant.fromEpochSeconds(entity.recordedAt.epochSecond, entity.recordedAt.nano),
            createdAt =
                checkNotNull(entity.createdAt).let {
                    Instant.fromEpochSeconds(it.epochSecond, it.nano)
                },
            cost =
                entity.price?.let {
                    CostSnapshot(
                        total = Money(it.movePointRight(2).longValueExact()),
                        unitPrice = entity.unitPriceMinor?.let(::Money),
                        priceBasisMilligrams = entity.priceBasisMilligrams,
                    )
                },
        )
}
