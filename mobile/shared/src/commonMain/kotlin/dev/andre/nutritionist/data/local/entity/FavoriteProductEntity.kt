package dev.andre.nutritionist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_products")
data class FavoriteProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    val name: String,
    val category: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    @ColumnInfo(name = "price_amount_minor") val priceAmountMinor: Long? = null,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
