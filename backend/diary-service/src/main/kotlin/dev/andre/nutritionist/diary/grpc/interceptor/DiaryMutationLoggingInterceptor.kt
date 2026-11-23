package dev.andre.nutritionist.diary.grpc.interceptor

import com.budgetntr.proto.diary.v1.DiaryServiceGrpc
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component

@Component
@GlobalServerInterceptor
class DiaryMutationLoggingInterceptor : ServerInterceptor {
    private val logger = LoggerFactory.getLogger(DiaryMutationLoggingInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val methodName = call.methodDescriptor.fullMethodName
        if (methodName !in mutationMethods) {
            return next.startCall(call, headers)
        }

        val hasIdempotencyKey = !headers.get(IDEMPOTENCY_KEY_HEADER).isNullOrBlank()
        logger.info(
            "Diary mutation started: method={}, idempotencyKeyPresent={}",
            methodName,
            hasIdempotencyKey,
        )

        val loggingCall =
            object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                override fun close(
                    status: Status,
                    trailers: Metadata,
                ) {
                    logger.info("Diary mutation finished: method={}, status={}", methodName, status.code)
                    super.close(status, trailers)
                }
            }

        return next.startCall(loggingCall, headers)
    }

    companion object {
        private val IDEMPOTENCY_KEY_HEADER =
            Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
        private val mutationMethods =
            setOf(
                DiaryServiceGrpc.getAddEntryMethod().fullMethodName,
                DiaryServiceGrpc.getDeleteMealMethod().fullMethodName,
            )
    }
}
