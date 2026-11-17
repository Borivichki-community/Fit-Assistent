package dev.andre.nutritionist.catalog.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class PriceKey(
    @Column(name = "product_id") val productId: UUID,
    @Column(name = "store_id") val storeId: UUID,
    val currency: String,
) : Serializable

@Entity
@Table(name = "prices")
class PriceEntity(
    @EmbeddedId val id: PriceKey,
    @Column(name = "unit_price_minor") val unitPriceMinor: Long,
    @Column(name = "price_basis_milligrams") val priceBasisMilligrams: Long,
    @Column(name = "updated_at") val updatedAt: Instant,
)
