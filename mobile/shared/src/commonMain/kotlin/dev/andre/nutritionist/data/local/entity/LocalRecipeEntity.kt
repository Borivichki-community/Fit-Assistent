package dev.andre.nutritionist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "local_recipes")
data class LocalRecipeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "author_type") val authorType: String,
    @ColumnInfo(name = "author_id") val authorId: String? = null,
    @ColumnInfo(name = "origin_recipe_id") val originRecipeId: String? = null,
    val title: String,
    val description: String? = null,
    val instructions: String? = null,
    val visibility: String,
    @ColumnInfo(name = "servings_count") val servingsCount: Int = 1,
    @ColumnInfo(name = "total_weight_g") val totalWeightGrams: Double = 0.0,
    @ColumnInfo(name = "calories_per_100g") val caloriesPer100g: Double = 0.0,
    @ColumnInfo(name = "proteins_per_100g") val proteinsPer100g: Double = 0.0,
    @ColumnInfo(name = "fats_per_100g") val fatsPer100g: Double = 0.0,
    @ColumnInfo(name = "carbs_per_100g") val carbsPer100g: Double = 0.0,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING",
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "local_recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = LocalRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recipe_id"])],
)
data class LocalRecipeIngredientEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    @ColumnInfo(name = "product_id") val productId: String? = null,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "weight_grams") val weightGrams: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
)

data class LocalRecipeWithIngredients(
    @Embedded val recipe: LocalRecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id",
    )
    val ingredients: List<LocalRecipeIngredientEntity>,
)
