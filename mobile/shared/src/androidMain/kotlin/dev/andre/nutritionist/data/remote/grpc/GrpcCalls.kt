package dev.andre.nutritionist.data.remote.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.TimeUnit

private const val DEFAULT_UNARY_DEADLINE_SECONDS = 20L
private const val DEFAULT_STREAM_DEADLINE_SECONDS = 90L

suspend fun <Req, Resp> Channel.unaryCall(
    method: MethodDescriptor<Req, Resp>,
    request: Req,
    callOptions: CallOptions = CallOptions.DEFAULT,
): Resp = suspendCancellableCoroutine { continuation ->
    val effectiveOptions = if (callOptions.deadline == null) {
        callOptions.withDeadlineAfter(DEFAULT_UNARY_DEADLINE_SECONDS, TimeUnit.SECONDS)
    } else {
        callOptions
    }
    val call = newCall(method, effectiveOptions)
    continuation.invokeOnCancellation {
        call.cancel("Coroutine cancelled", it)
    }
    ClientCalls.asyncUnaryCall(call, request, object : StreamObserver<Resp> {
        override fun onNext(value: Resp) {
            continuation.resume(value)
        }

        override fun onError(t: Throwable) {
            continuation.resumeWithException(t)
        }

        override fun onCompleted() {}
    })
}

fun <Req, Resp> Channel.serverStreamingCall(
    method: MethodDescriptor<Req, Resp>,
    request: Req,
    callOptions: CallOptions = CallOptions.DEFAULT,
): Flow<Resp> = callbackFlow {
    val effectiveOptions = if (callOptions.deadline == null) {
        callOptions.withDeadlineAfter(DEFAULT_STREAM_DEADLINE_SECONDS, TimeUnit.SECONDS)
    } else {
        callOptions
    }
    val call = newCall(method, effectiveOptions)
    ClientCalls.asyncServerStreamingCall(call, request, object : StreamObserver<Resp> {
        override fun onNext(value: Resp) {
            trySend(value)
        }

        override fun onError(t: Throwable) {
            close(t)
        }

        override fun onCompleted() {
            close()
        }
    })
    awaitClose {
        call.cancel("Flow collection cancelled", null)
    }
}
