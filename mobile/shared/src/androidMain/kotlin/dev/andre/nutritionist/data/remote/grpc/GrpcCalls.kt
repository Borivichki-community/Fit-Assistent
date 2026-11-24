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

suspend fun <Req, Resp> Channel.unaryCall(
    method: MethodDescriptor<Req, Resp>,
    request: Req,
    callOptions: CallOptions = CallOptions.DEFAULT,
): Resp = suspendCancellableCoroutine { continuation ->
    val call = newCall(method, callOptions)
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
    val call = newCall(method, callOptions)
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
