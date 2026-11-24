package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.catalog.v1.AveragePriceItemProto
import com.budgetntr.proto.catalog.v1.CatalogNutritionFactsProto
import com.budgetntr.proto.catalog.v1.GetAveragePricesRequest
import com.budgetntr.proto.catalog.v1.GetAveragePricesResponse
import com.budgetntr.proto.catalog.v1.GetProductRequest
import com.budgetntr.proto.catalog.v1.GetProductResponse
import com.budgetntr.proto.catalog.v1.ProductItemProto
import com.budgetntr.proto.catalog.v1.RegionalPriceItemProto
import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import com.budgetntr.proto.catalog.v1.SearchProductsResponse
import com.budgetntr.proto.profile.v1.TimestampProto
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import io.grpc.Channel
import io.grpc.MethodDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AndroidCatalogGrpcServiceClient(
    private val channel: Channel,
) : CatalogGrpcServiceClient {

    override suspend fun searchProducts(request: SearchProductsRequest): SearchProductsResponse {
        return channel.unaryCall(searchProductsMethod, request)
    }

    override suspend fun getProduct(request: GetProductRequest): GetProductResponse {
        return channel.unaryCall(getProductMethod, request)
    }

    override suspend fun getAveragePrices(request: GetAveragePricesRequest): GetAveragePricesResponse {
        return channel.unaryCall(getAveragePricesMethod, request)
    }

    companion object {
        private const val SERVICE_NAME = "nutritionist.catalog.v1.CatalogService"

        val searchProductsMethod: MethodDescriptor<SearchProductsRequest, SearchProductsResponse> =
            MethodDescriptor.newBuilder<SearchProductsRequest, SearchProductsResponse>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "SearchProducts"))
                .setRequestMarshaller(SearchProductsRequestMarshaller)
                .setResponseMarshaller(SearchProductsResponseMarshaller)
                .build()

        val getProductMethod: MethodDescriptor<GetProductRequest, GetProductResponse> =
            MethodDescriptor.newBuilder<GetProductRequest, GetProductResponse>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "GetProduct"))
                .setRequestMarshaller(GetProductRequestMarshaller)
                .setResponseMarshaller(GetProductResponseMarshaller)
                .build()

        val getAveragePricesMethod: MethodDescriptor<GetAveragePricesRequest, GetAveragePricesResponse> =
            MethodDescriptor.newBuilder<GetAveragePricesRequest, GetAveragePricesResponse>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "GetAveragePrices"))
                .setRequestMarshaller(GetAveragePricesRequestMarshaller)
                .setResponseMarshaller(GetAveragePricesResponseMarshaller)
                .build()
    }
}

internal object SearchProductsRequestMarshaller : MethodDescriptor.Marshaller<SearchProductsRequest> {
    override fun stream(value: SearchProductsRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.query.isNotEmpty()) cos.writeString(1, value.query)
        if (value.category.isNotEmpty()) cos.writeString(2, value.category)
        value.minCalories?.let { cos.writeDouble(3, it) }
        value.maxCalories?.let { cos.writeDouble(4, it) }
        value.minProteins?.let { cos.writeDouble(5, it) }
        value.regionId?.let { cos.writeString(6, it) }
        if (value.pageSize != 0) cos.writeInt32(7, value.pageSize)
        if (value.pageToken.isNotEmpty()) cos.writeString(8, value.pageToken)
        value.maxProteins?.let { cos.writeDouble(9, it) }
        value.sortBy?.let { cos.writeString(10, it) }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): SearchProductsRequest {
        val cis = CodedInputStream.newInstance(stream)
        var query = ""
        var category = ""
        var minCalories: Double? = null
        var maxCalories: Double? = null
        var minProteins: Double? = null
        var regionId: String? = null
        var pageSize = 0
        var pageToken = ""
        var maxProteins: Double? = null
        var sortBy: String? = null

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> query = cis.readString()
                (2 shl 3) or 2 -> category = cis.readString()
                (3 shl 3) or 1 -> minCalories = cis.readDouble()
                (4 shl 3) or 1 -> maxCalories = cis.readDouble()
                (5 shl 3) or 1 -> minProteins = cis.readDouble()
                (6 shl 3) or 2 -> regionId = cis.readString()
                (7 shl 3) or 0 -> pageSize = cis.readInt32()
                (8 shl 3) or 2 -> pageToken = cis.readString()
                (9 shl 3) or 1 -> maxProteins = cis.readDouble()
                (10 shl 3) or 2 -> sortBy = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return SearchProductsRequest(
            query = query,
            category = category,
            minCalories = minCalories,
            maxCalories = maxCalories,
            minProteins = minProteins,
            regionId = regionId,
            pageSize = pageSize,
            pageToken = pageToken,
            maxProteins = maxProteins,
            sortBy = sortBy,
        )
    }
}

internal object SearchProductsResponseMarshaller : MethodDescriptor.Marshaller<SearchProductsResponse> {
    override fun stream(value: SearchProductsResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        for (product in value.products) {
            val productBytes = ProductItemMarshallerHelper.toByteArray(product)
            cos.writeByteArray(1, productBytes)
        }
        if (value.nextPageToken.isNotEmpty()) cos.writeString(2, value.nextPageToken)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): SearchProductsResponse {
        val cis = CodedInputStream.newInstance(stream)
        val products = mutableListOf<ProductItemProto>()
        var nextPageToken = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> {
                    val bytes = cis.readByteArray()
                    products.add(ProductItemMarshallerHelper.fromByteArray(bytes))
                }
                (2 shl 3) or 2 -> nextPageToken = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return SearchProductsResponse(products = products, nextPageToken = nextPageToken)
    }
}

internal object GetProductRequestMarshaller : MethodDescriptor.Marshaller<GetProductRequest> {
    override fun stream(value: GetProductRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.productId.isNotEmpty()) cos.writeString(1, value.productId)
        value.regionId?.let { cos.writeString(2, it) }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetProductRequest {
        val cis = CodedInputStream.newInstance(stream)
        var productId = ""
        var regionId: String? = null
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> productId = cis.readString()
                (2 shl 3) or 2 -> regionId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GetProductRequest(productId = productId, regionId = regionId)
    }
}

internal object GetProductResponseMarshaller : MethodDescriptor.Marshaller<GetProductResponse> {
    override fun stream(value: GetProductResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        val bytes = ProductItemMarshallerHelper.toByteArray(value.product)
        cos.writeByteArray(1, bytes)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetProductResponse {
        val cis = CodedInputStream.newInstance(stream)
        var product = ProductItemProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> {
                    val bytes = cis.readByteArray()
                    product = ProductItemMarshallerHelper.fromByteArray(bytes)
                }
                else -> cis.skipField(tag)
            }
        }
        return GetProductResponse(product = product)
    }
}

internal object GetAveragePricesRequestMarshaller : MethodDescriptor.Marshaller<GetAveragePricesRequest> {
    override fun stream(value: GetAveragePricesRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        for (id in value.productIds) {
            cos.writeString(1, id)
        }
        value.regionId?.let { cos.writeString(2, it) }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetAveragePricesRequest {
        val cis = CodedInputStream.newInstance(stream)
        val ids = mutableListOf<String>()
        var regionId: String? = null
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> ids.add(cis.readString())
                (2 shl 3) or 2 -> regionId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GetAveragePricesRequest(productIds = ids, regionId = regionId)
    }
}

internal object GetAveragePricesResponseMarshaller : MethodDescriptor.Marshaller<GetAveragePricesResponse> {
    override fun stream(value: GetAveragePricesResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        for (price in value.prices) {
            val itemBaos = ByteArrayOutputStream()
            val itemCos = CodedOutputStream.newInstance(itemBaos)
            if (price.productId.isNotEmpty()) itemCos.writeString(1, price.productId)
            if (price.averagePrice != 0.0) itemCos.writeDouble(2, price.averagePrice)
            if (price.currency.isNotEmpty()) itemCos.writeString(3, price.currency)
            if (price.regionCount != 0) itemCos.writeInt32(4, price.regionCount)
            itemCos.flush()
            cos.writeByteArray(1, itemBaos.toByteArray())
        }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetAveragePricesResponse {
        val cis = CodedInputStream.newInstance(stream)
        val prices = mutableListOf<AveragePriceItemProto>()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> {
                    val itemBytes = cis.readByteArray()
                    val itemCis = CodedInputStream.newInstance(itemBytes)
                    var productId = ""
                    var averagePrice = 0.0
                    var currency = "RUB"
                    var regionCount = 0
                    while (true) {
                        val itemTag = itemCis.readTag()
                        if (itemTag == 0) break
                        when (itemTag) {
                            (1 shl 3) or 2 -> productId = itemCis.readString()
                            (2 shl 3) or 1 -> averagePrice = itemCis.readDouble()
                            (3 shl 3) or 2 -> currency = itemCis.readString()
                            (4 shl 3) or 0 -> regionCount = itemCis.readInt32()
                            else -> itemCis.skipField(itemTag)
                        }
                    }
                    prices.add(
                        AveragePriceItemProto(
                            productId = productId,
                            averagePrice = averagePrice,
                            currency = currency,
                            regionCount = regionCount,
                        )
                    )
                }
                else -> cis.skipField(tag)
            }
        }
        return GetAveragePricesResponse(prices = prices)
    }
}

internal object ProductItemMarshallerHelper {
    fun toByteArray(value: ProductItemProto): ByteArray {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.name.isNotEmpty()) cos.writeString(2, value.name)
        if (value.barcode.isNotEmpty()) cos.writeString(3, value.barcode)
        if (value.category.isNotEmpty()) cos.writeString(4, value.category)

        // nutrition: 5
        val nutBaos = ByteArrayOutputStream()
        val nutCos = CodedOutputStream.newInstance(nutBaos)
        if (value.nutrition.caloriesPer100g != 0.0) nutCos.writeDouble(1, value.nutrition.caloriesPer100g)
        if (value.nutrition.proteinsPer100g != 0.0) nutCos.writeDouble(2, value.nutrition.proteinsPer100g)
        if (value.nutrition.fatsPer100g != 0.0) nutCos.writeDouble(3, value.nutrition.fatsPer100g)
        if (value.nutrition.carbsPer100g != 0.0) nutCos.writeDouble(4, value.nutrition.carbsPer100g)
        nutCos.flush()
        cos.writeByteArray(5, nutBaos.toByteArray())

        if (value.version != 0L) cos.writeInt64(6, value.version)

        // created_at: 7
        val tsBaos = ByteArrayOutputStream()
        val tsCos = CodedOutputStream.newInstance(tsBaos)
        if (value.createdAt.seconds != 0L) tsCos.writeInt64(1, value.createdAt.seconds)
        if (value.createdAt.nanos != 0) tsCos.writeInt32(2, value.createdAt.nanos)
        tsCos.flush()
        cos.writeByteArray(7, tsBaos.toByteArray())

        // prices: 8 (repeated)
        for (price in value.prices) {
            val priceBaos = ByteArrayOutputStream()
            val priceCos = CodedOutputStream.newInstance(priceBaos)
            if (price.id.isNotEmpty()) priceCos.writeString(1, price.id)
            if (price.productId.isNotEmpty()) priceCos.writeString(2, price.productId)
            if (price.regionId.isNotEmpty()) priceCos.writeString(3, price.regionId)
            if (price.price != 0.0) priceCos.writeDouble(4, price.price)
            if (price.currency.isNotEmpty()) priceCos.writeString(5, price.currency)
            if (price.unit.isNotEmpty()) priceCos.writeString(6, price.unit)
            if (price.regionName.isNotEmpty()) priceCos.writeString(8, price.regionName)
            priceCos.flush()
            cos.writeByteArray(8, priceBaos.toByteArray())
        }

        cos.flush()
        return baos.toByteArray()
    }

    fun fromByteArray(bytes: ByteArray): ProductItemProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var name = ""
        var barcode = ""
        var category = ""
        var nutrition = CatalogNutritionFactsProto()
        var version = 0L
        var createdAt = TimestampProto()
        val prices = mutableListOf<RegionalPriceItemProto>()

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> name = cis.readString()
                (3 shl 3) or 2 -> barcode = cis.readString()
                (4 shl 3) or 2 -> category = cis.readString()
                (5 shl 3) or 2 -> {
                    val nutBytes = cis.readByteArray()
                    val nutCis = CodedInputStream.newInstance(nutBytes)
                    var c = 0.0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val nutTag = nutCis.readTag()
                        if (nutTag == 0) break
                        when (nutTag) {
                            (1 shl 3) or 1 -> c = nutCis.readDouble()
                            (2 shl 3) or 1 -> p = nutCis.readDouble()
                            (3 shl 3) or 1 -> f = nutCis.readDouble()
                            (4 shl 3) or 1 -> cb = nutCis.readDouble()
                            else -> nutCis.skipField(nutTag)
                        }
                    }
                    nutrition = CatalogNutritionFactsProto(caloriesPer100g = c, proteinsPer100g = p, fatsPer100g = f, carbsPer100g = cb)
                }
                (6 shl 3) or 0 -> version = cis.readInt64()
                (7 shl 3) or 2 -> {
                    val tsBytes = cis.readByteArray()
                    val tsCis = CodedInputStream.newInstance(tsBytes)
                    var sec = 0L
                    var nanos = 0
                    while (true) {
                        val tsTag = tsCis.readTag()
                        if (tsTag == 0) break
                        when (tsTag) {
                            (1 shl 3) or 0 -> sec = tsCis.readInt64()
                            (2 shl 3) or 0 -> nanos = tsCis.readInt32()
                            else -> tsCis.skipField(tsTag)
                        }
                    }
                    createdAt = TimestampProto(seconds = sec, nanos = nanos)
                }
                (8 shl 3) or 2 -> {
                    val priceBytes = cis.readByteArray()
                    val priceCis = CodedInputStream.newInstance(priceBytes)
                    var prId = ""
                    var prodId = ""
                    var regId = ""
                    var price = 0.0
                    var curr = "RUB"
                    var unit = "кг"
                    var regName = ""
                    while (true) {
                        val priceTag = priceCis.readTag()
                        if (priceTag == 0) break
                        when (priceTag) {
                            (1 shl 3) or 2 -> prId = priceCis.readString()
                            (2 shl 3) or 2 -> prodId = priceCis.readString()
                            (3 shl 3) or 2 -> regId = priceCis.readString()
                            (4 shl 3) or 1 -> price = priceCis.readDouble()
                            (5 shl 3) or 2 -> curr = priceCis.readString()
                            (6 shl 3) or 2 -> unit = priceCis.readString()
                            (8 shl 3) or 2 -> regName = priceCis.readString()
                            else -> priceCis.skipField(priceTag)
                        }
                    }
                    prices.add(
                        RegionalPriceItemProto(
                            id = prId,
                            productId = prodId,
                            regionId = regId,
                            price = price,
                            currency = curr,
                            unit = unit,
                            regionName = regName,
                        )
                    )
                }
                else -> cis.skipField(tag)
            }
        }
        return ProductItemProto(
            id = id,
            name = name,
            barcode = barcode,
            category = category,
            nutrition = nutrition,
            version = version,
            createdAt = createdAt,
            prices = prices,
        )
    }
}
