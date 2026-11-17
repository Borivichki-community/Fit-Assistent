package dev.andre.nutritionist.profile.grpc.interceptor

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component

private const val DAILY_CALORIES_IDENTITY_ORDER = Ordered.HIGHEST_PRECEDENCE + 5

@Component
@GlobalServerInterceptor
@Order(DAILY_CALORIES_IDENTITY_ORDER)
class DailyCaloriesIdentityInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> =
        if (call.methodDescriptor.fullMethodName.endsWith("/GetDailyCaloriesBalance")) {
            val userId = headers.get(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER))
            if (userId.isNullOrBlank()) {
                call.close(Status.UNAUTHENTICATED.withDescription("Trusted user identity is required"), Metadata())
                object : ServerCall.Listener<ReqT>() {}
            } else {
                Contexts.interceptCall(Context.current().withValue(USER_ID, userId), call, headers, next)
            }
        } else {
            next.startCall(call, headers)
        }

    companion object {
        val USER_ID: Context.Key<String> = Context.key("profile-calories-user")
    }
}
