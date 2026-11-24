package dev.andre.nutritionist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "local_meal_plans")
data class LocalMealPlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "author_id") val authorId: String,
    @ColumnInfo(name = "author_type") val authorType: String,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "duration_days") val durationDays: Int = 1,
    val visibility: String,
    @ColumnInfo(name = "access_mode") val accessMode: String,
    @ColumnInfo(name = "price_amount_minor") val priceAmountMinor: Long = 0L,
    val currency: String = "RUB",
    @ColumnInfo(name = "avg_daily_calories") val avgDailyCalories: Double = 0.0,
    @ColumnInfo(name = "avg_daily_proteins") val avgDailyProteins: Double = 0.0,
    @ColumnInfo(name = "avg_daily_fats") val avgDailyFats: Double = 0.0,
    @ColumnInfo(name = "avg_daily_carbs") val avgDailyCarbs: Double = 0.0,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "SYNCED",
    @ColumnInfo(name = "updated_at") val updatedAtEpochMs: Long = 0L,
)

@Entity(
    tableName = "local_meal_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = LocalMealPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["plan_id"])],
)
data class LocalMealPlanItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "day_number") val dayNumber: Int,
    @ColumnInfo(name = "meal_slot") val mealSlot: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "name_snapshot") val nameSnapshot: String,
    @ColumnInfo(name = "weight_grams") val weightGrams: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    @ColumnInfo(name = "estimated_cost_minor") val estimatedCostMinor: Long? = null,
)

data class LocalMealPlanWithItems(
    @Embedded val plan: LocalMealPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "plan_id",
    )
    val items: List<LocalMealPlanItemEntity>,
)
