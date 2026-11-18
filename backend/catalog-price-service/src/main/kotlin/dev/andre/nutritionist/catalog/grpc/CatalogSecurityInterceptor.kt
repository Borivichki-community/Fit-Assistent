package dev.andre.nutritionist.catalog.grpc

import dev.andre.nutritionist.catalog.domain.IdempotencyKey
import dev.andre.nutritionist.catalog.domain.UserId
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component

data class CatalogPrincipal(
    val userId: UserId,
    val admin: Boolean,
    val idempotencyKey: IdempotencyKey?,
)

@Component
@GlobalServerInterceptor
class CatalogSecurityInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        if (!call.methodDescriptor.fullMethodName.startsWith("com.budgetntr.catalog.v1.CatalogService/")) {
            return next.startCall(call, headers)
        }
        val subject = headers.get(header("x-user-id"))?.takeIf { it.isNotBlank() && it.length <= 255 }
        if (subject == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("Verified gateway identity required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        val roles =
            headers
                .get(header("x-user-roles"))
                .orEmpty()
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map { it.trim().trim('"') }
        val principal =
            CatalogPrincipal(
                UserId(subject),
                roles.any { it == "admin" || it == "ROLE_ADMIN" },
                headers.get(header("idempotency-key"))?.let(::IdempotencyKey),
            )
        return Contexts.interceptCall(Context.current().withValue(PRINCIPAL, principal), call, headers, next)
    }

    companion object {
        val PRINCIPAL: Context.Key<CatalogPrincipal> = Context.key("catalog-principal")

        private fun header(name: String): Metadata.Key<String> = Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER)
    }
}
