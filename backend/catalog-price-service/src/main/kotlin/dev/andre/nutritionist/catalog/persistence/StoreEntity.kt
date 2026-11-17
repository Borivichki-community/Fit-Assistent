package dev.andre.nutritionist.catalog.persistence

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "stores")
class StoreEntity(
    @Id val id: UUID,
    val name: String,
)
