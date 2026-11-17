package dev.andre.nutritionist.catalog.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "products")
class ProductEntity(
    @Id val id: UUID,
    val name: String,
    val category: String,
    @Column(name = "calories_per_100g", precision = 7, scale = 2)
    val caloriesPer100g: BigDecimal,
    @Column(name = "proteins_per_100g", precision = 5, scale = 2)
    val proteinsPer100g: BigDecimal,
    @Column(name = "fats_per_100g", precision = 5, scale = 2)
    val fatsPer100g: BigDecimal,
    @Column(name = "carbs_per_100g", precision = 5, scale = 2)
    val carbsPer100g: BigDecimal,
)
