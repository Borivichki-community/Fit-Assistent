package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.GenerateWeekMenuRequest
import com.budgetntr.proto.recipe.v1.GenerateWeekMenuResponse
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpc
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.server.service.GrpcService
import org.springframework.stereotype.Component
import java.util.UUID
import javax.net.ssl.SSLPeerUnverifiedException
import javax.security.auth.x500.X500Principal

@GrpcService
class RecipeGrpcService(
    private val recipe: RecipeMenuService,
) : RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun generateWeekMenu(request: GenerateWeekMenuRequest): Flow<GenerateWeekMenuResponse> {
        val context = RecipeIdentityInterceptor.requestContext.get() ?: throw Status.UNAUTHENTICATED.asException()
        val logging =
            buildMap {
                put("userId", context.user.raw)
                context.traceparent?.split('-')?.let { trace ->
                    put("traceId", trace[1])
                    put("spanId", trace[2])
                }
            }
        return flow {
            val input =
                MenuRequest(
                    request.dailyCalories,
                    request.weeklyBudgetMinor,
                    productIds(request.excludedProductIdsList).sortedBy { it.raw },
                    productIds(request.preferredProductIdsList),
                )
            val menu = recipe.generate(input, context)
            menu.daysList.forEach { emit(GenerateWeekMenuResponse.newBuilder().setDayReady(it).build()) }
            emit(GenerateWeekMenuResponse.newBuilder().setFinal(menu.summary).build())
        }.catch { failure ->
            val status =
                when (failure) {
                    is CancellationException -> {
                        throw failure
                    }

                    is IllegalArgumentException -> {
                        Status.INVALID_ARGUMENT.withDescription(failure.message)
                    }

                    is QuotaExceeded -> {
                        Status.RESOURCE_EXHAUSTED.withDescription(failure.message)
                    }

                    is MenuUnavailable -> {
                        Status.UNAVAILABLE.withDescription(failure.message)
                    }

                    else -> {
                        logger.error("Recipe generation failed: {}", failure.javaClass.simpleName)
                        Status.INTERNAL.withDescription("Recipe generation failed")
                    }
                }
            throw status.asException()
        }.flowOn(MDCContext(logging))
    }

    private fun productIds(ids: List<String>): List<ProductId> {
        require(ids.size <= 50) { "At most 50 product IDs per list" }
        return ids
            .map {
                require(uuidPattern.matches(it)) { "Product IDs must be UUIDs" }
                ProductId(UUID.fromString(it).toString())
            }.distinct()
    }

    companion object {
        private val uuidPattern = Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    }
}

@Component
@GlobalServerInterceptor
class RecipeIdentityInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        if (call.methodDescriptor.serviceName != RecipeAiServiceGrpc.SERVICE_NAME) {
            return next.startCall(call, headers)
        }
        val session = call.attributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION)
        val peer =
            try {
                session?.peerPrincipal
            } catch (_: SSLPeerUnverifiedException) {
                null
            }
        val subject = headers.getAll(CatalogClient.userHeader)?.singleOrNull()?.takeIf { it.isNotBlank() && it.length <= 255 }
        if (peer != gatewayPrincipal || subject == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("Authenticated gateway and identity required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        val trace = headers.get(CatalogClient.traceHeader)?.takeIf(tracePattern::matches)
        val context = Context.current().withValue(requestContext, RequestContext(UserId(subject), trace))
        return Contexts.interceptCall(context, call, headers, next)
    }

    companion object {
        internal val requestContext: Context.Key<RequestContext> = Context.key("recipe-request")
        private val gatewayPrincipal = X500Principal("CN=fitassistant-envoy")
        private val tracePattern = Regex("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}")
    }
}
