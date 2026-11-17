package dev.andre.nutritionist.catalog.grpc

import com.budgetntr.proto.catalog.v1.SearchProductsRequest
import dev.andre.nutritionist.catalog.domain.ProductId
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

object SearchCursor {
    fun decode(request: SearchProductsRequest): ProductId? {
        if (request.pageToken.isEmpty()) return null
        require(request.pageToken.length <= 256) { "Invalid page token" }
        val parts = String(Base64.getUrlDecoder().decode(request.pageToken), Charsets.UTF_8).split("|")
        require(parts.size == 3 && parts[0] == "1" && parts[2] == fingerprint(request)) { "Token does not match filters" }
        val id = UUID.fromString(parts[1]).toString()
        require(id == parts[1]) { "Invalid page token ID" }
        return ProductId(id)
    }

    fun encode(
        id: ProductId,
        request: SearchProductsRequest,
    ): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString("1|${id.raw}|${fingerprint(request)}".toByteArray(Charsets.UTF_8))

    private fun fingerprint(request: SearchProductsRequest): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                request
                    .toBuilder()
                    .clearPageToken()
                    .setPageSize(0)
                    .build()
                    .toByteArray(),
            ).joinToString("") { "%02x".format(it) }
}
