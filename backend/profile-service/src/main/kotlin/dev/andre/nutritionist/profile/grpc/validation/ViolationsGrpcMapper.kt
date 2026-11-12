package dev.andre.nutritionist.profile.grpc.validation

import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import com.google.protobuf.Any as ProtoAny

/**
 * Преобразует список нарушений валидации в StatusException со стандартными
 * деталями google.rpc.BadRequest (gRPC Rich Error Model).
 */
fun List<Violation>.toStatusException(): StatusException {
    val badRequest =
        BadRequest
            .newBuilder()
            .also { builder ->
                forEach {
                    builder.addFieldViolations(
                        BadRequest.FieldViolation
                            .newBuilder()
                            .setField(it.field)
                            .setReason(it.code.name)
                            .setDescription(it.message),
                    )
                }
            }.build()

    val status =
        com.google.rpc.Status
            .newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage("Request validation failed: $size violation(s)")
            .addDetails(ProtoAny.pack(badRequest))
            .build()

    return StatusProto.toStatusException(status)
}
