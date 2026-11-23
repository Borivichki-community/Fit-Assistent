package dev.andre.nutritionist.catalog.cache.model

import com.budgetntr.proto.catalog.v1.AveragePriceItem

data class CachedAveragePriceDto(
    val productId: String = "",
    val averagePrice: Double = 0.0,
    val currency: String = "RUB",
    val regionCount: Int = 0,
) {
    fun toProto(): AveragePriceItem =
        AveragePriceItem
            .newBuilder()
            .setProductId(productId)
            .setAveragePrice(averagePrice)
            .setCurrency(currency)
            .setRegionCount(regionCount)
            .build()

    companion object {
        fun fromProto(proto: AveragePriceItem): CachedAveragePriceDto =
            CachedAveragePriceDto(
                productId = proto.productId,
                averagePrice = proto.averagePrice,
                currency = proto.currency,
                regionCount = proto.regionCount,
            )
    }
}
