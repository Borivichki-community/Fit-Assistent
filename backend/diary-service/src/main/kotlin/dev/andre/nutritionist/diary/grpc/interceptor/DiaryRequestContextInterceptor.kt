package dev.andre.nutritionist.diary.grpc.interceptor

import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.UserId
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component

@Component
@GlobalServerInterceptor
@Order(100)
class DiaryRequestContextInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        if (method.substringBeforeLast('/') != DiaryServiceGrpc.SERVICE_NAME) {
            return next.startCall(call, headers)
        }

        val userId = headers.get(USER_ID_HEADER)
        if (userId.isNullOrBlank() || userId.length > 64) {
            call.close(Status.UNAUTHENTICATED.withDescription("Trusted user context is required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        val key = headers.get(IDEMPOTENCY_KEY_HEADER)
        if (method in mutationMethods && (key.isNullOrBlank() || key.length > 64)) {
            call.close(Status.INVALID_ARGUMENT.withDescription("Valid idempotency-key is required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        val context = Context.current().withValue(USER_ID_CONTEXT, UserId(userId))
        val requestContext = if (key == null) context else context.withValue(KEY_CONTEXT, IdempotencyKey(key))
        return Contexts.interceptCall(requestContext, call, headers, next)
    }

    companion object {
        private val USER_ID_HEADER = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)
        private val IDEMPOTENCY_KEY_HEADER = Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
        private val USER_ID_CONTEXT = Context.key<UserId>("diary-user-id")
        private val KEY_CONTEXT = Context.key<IdempotencyKey>("diary-idempotency-key")
        private val mutationMethods =
            setOf(
                DiaryServiceGrpc.getAddEntryMethod().fullMethodName,
                DiaryServiceGrpc.getDeleteMealMethod().fullMethodName,
            )

        fun currentUserId(): UserId =
            USER_ID_CONTEXT.get()
                ?: throw Status.UNAUTHENTICATED.withDescription("User context is missing").asRuntimeException()

        fun currentKey(): IdempotencyKey =
            KEY_CONTEXT.get()
                ?: throw Status.INVALID_ARGUMENT.withDescription("idempotency-key is missing").asRuntimeException()
    }
}
