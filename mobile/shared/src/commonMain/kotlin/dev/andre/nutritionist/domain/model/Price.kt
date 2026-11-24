package dev.andre.nutritionist.domain.model

data class Price(
    val amount: Money,
    val unit: String = "кг",
)

data class RegionalPrice(
    val id: String,
    val productId: ProductId,
    val regionId: RegionId,
    val price: Price,
    val regionName: String = "",
)
