package dev.andre.nutritionist.diary.domain.model

data class CostSnapshot(
    val total: Money,
    val unitPrice: Money? = null,
    val priceBasisMilligrams: Long? = null,
)
