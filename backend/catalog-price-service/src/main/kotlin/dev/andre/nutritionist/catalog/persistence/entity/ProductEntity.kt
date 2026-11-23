package dev.andre.nutritionist.catalog.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products")
@NamedEntityGraph(
    name = "Product.withPricesAndRegions",
    attributeNodes = [
        NamedAttributeNode(value = "prices", subgraph = "prices-subgraph"),
    ],
    subgraphs = [
        NamedSubgraph(name = "prices-subgraph", attributeNodes = [NamedAttributeNode("region")]),
    ],
)
class ProductEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Column(name = "barcode", length = 64, unique = true)
    var barcode: String? = null,
    @Column(name = "category", nullable = false, length = 64)
    var category: String = "OTHER",
    @Column(name = "calories_per_100g", nullable = false, precision = 6, scale = 2)
    var caloriesPer100g: BigDecimal,
    @Column(name = "proteins_per_100g", nullable = false, precision = 6, scale = 2)
    var proteinsPer100g: BigDecimal,
    @Column(name = "fats_per_100g", nullable = false, precision = 6, scale = 2)
    var fatsPer100g: BigDecimal,
    @Column(name = "carbs_per_100g", nullable = false, precision = 6, scale = 2)
    var carbsPer100g: BigDecimal,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @org.hibernate.annotations.BatchSize(size = 50)
    @OneToMany(
        mappedBy = "product",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val prices: MutableList<RegionalPriceEntity> = mutableListOf(),
)
