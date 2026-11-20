package dev.andre.nutritionist.catalog.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "regional_prices",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_product_region", columnNames = ["product_id", "region_id"]),
    ],
)
class RegionalPriceEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    var region: RegionEntity,
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "RUB",
    @Column(name = "unit", nullable = false, length = 30)
    var unit: String = "PIECE",
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
