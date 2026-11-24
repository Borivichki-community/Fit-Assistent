package dev.andre.nutritionist.diary.persistence.entity

import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.EntrySourceType
import dev.andre.nutritionist.diary.domain.model.FoodItemId
import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "diary_entries",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_diary_request",
            columnNames = ["user_id", "idempotency_key"],
        ),
    ],
)
class DiaryEntryEntity(
    @field:Id
    @field:Column(name = "id", nullable = false)
    val id: DiaryEntryId,
    @field:Column(name = "user_id", nullable = false, length = 64)
    val userId: UserId,
    @field:Column(name = "idempotency_key", nullable = false, length = 64)
    val idempotencyKey: IdempotencyKey,
    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "source_type", nullable = false, length = 32)
    val sourceType: EntrySourceType = EntrySourceType.PRODUCT,
    @field:Column(name = "source_id")
    val sourceId: UUID? = null,
    @field:Column(name = "name_snapshot", nullable = false, length = 255)
    val nameSnapshot: String,
    @field:Column(name = "weight_grams", nullable = false, precision = 10, scale = 3)
    val weightGrams: BigDecimal,
    @field:Column(name = "calories", nullable = false, precision = 7, scale = 2)
    val calories: BigDecimal,
    @field:Column(name = "proteins", nullable = false, precision = 6, scale = 2)
    val proteins: BigDecimal,
    @field:Column(name = "fats", nullable = false, precision = 6, scale = 2)
    val fats: BigDecimal,
    @field:Column(name = "carbs", nullable = false, precision = 6, scale = 2)
    val carbs: BigDecimal,
    @field:Enumerated(EnumType.STRING)
    @field:Column(name = "meal_type", nullable = false, length = 20)
    val mealType: MealType,
    @field:Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant,
    @field:Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: Instant? = null,
    @field:Column(name = "price", precision = 10, scale = 2)
    val price: BigDecimal? = null,
    @field:Column(name = "unit_price_minor")
    val unitPriceMinor: Long? = null,
    @field:Column(name = "price_basis_milligrams")
    val priceBasisMilligrams: Long? = null,
    @field:Column(name = "food_item_id")
    val foodItemId: UUID? = null,
    @field:Column(name = "food_name")
    val foodName: String? = null,
    @field:Column(name = "quantity_grams", precision = 10, scale = 3)
    val quantityGrams: BigDecimal? = null,
) {
    constructor(
        id: DiaryEntryId,
        userId: UserId,
        idempotencyKey: IdempotencyKey,
        foodItemId: FoodItemId,
        foodName: String,
        quantityGrams: BigDecimal,
        calories: BigDecimal,
        proteins: BigDecimal,
        fats: BigDecimal,
        carbs: BigDecimal,
        mealType: MealType,
        recordedAt: Instant,
        createdAt: Instant? = null,
        price: BigDecimal? = null,
        unitPriceMinor: Long? = null,
        priceBasisMilligrams: Long? = null,
    ) : this(
        id = id,
        userId = userId,
        idempotencyKey = idempotencyKey,
        sourceType = EntrySourceType.PRODUCT,
        sourceId = foodItemId.raw,
        nameSnapshot = foodName,
        weightGrams = quantityGrams,
        calories = calories,
        proteins = proteins,
        fats = fats,
        carbs = carbs,
        mealType = mealType,
        recordedAt = recordedAt,
        createdAt = createdAt,
        price = price,
        unitPriceMinor = unitPriceMinor,
        priceBasisMilligrams = priceBasisMilligrams,
        foodItemId = foodItemId.raw,
        foodName = foodName,
        quantityGrams = quantityGrams,
    )
}
