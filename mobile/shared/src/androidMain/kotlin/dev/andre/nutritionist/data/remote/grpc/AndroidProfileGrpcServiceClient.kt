package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.profile.v1.ActivityLevelProto
import com.budgetntr.proto.profile.v1.GenderProto
import com.budgetntr.proto.profile.v1.GetProfileRequest
import com.budgetntr.proto.profile.v1.GoalTypeProto
import com.budgetntr.proto.profile.v1.NutritionGoalProto
import com.budgetntr.proto.profile.v1.ProfileProto
import com.budgetntr.proto.profile.v1.TimestampProto
import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android-реализация gRPC клиента профиля.
 * Выполняет реальные сетевые вызовы к ProfileService через [channel] с использованием
 * бинарного Protobuf-кодирования (wire format).
 */
class AndroidProfileGrpcServiceClient(
    private val channel: ManagedChannel,
) : ProfileGrpcServiceClient {

    override suspend fun getProfile(request: GetProfileRequest): ProfileProto {
        return channel.unaryCall(getProfileMethod, request)
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): ProfileProto {
        return channel.unaryCall(updateProfileMethod, request)
    }

    companion object {
        private const val SERVICE_NAME = "com.budgetntr.profile.v1.ProfileService"

        val getProfileMethod: MethodDescriptor<GetProfileRequest, ProfileProto> =
            MethodDescriptor.newBuilder<GetProfileRequest, ProfileProto>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "GetProfile"))
                .setRequestMarshaller(GetProfileRequestMarshaller)
                .setResponseMarshaller(ProfileProtoMarshaller)
                .build()

        val updateProfileMethod: MethodDescriptor<UpdateProfileRequest, ProfileProto> =
            MethodDescriptor.newBuilder<UpdateProfileRequest, ProfileProto>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "UpdateProfile"))
                .setRequestMarshaller(UpdateProfileRequestMarshaller)
                .setResponseMarshaller(ProfileProtoMarshaller)
                .build()
    }
}

private suspend fun <Req, Resp> ManagedChannel.unaryCall(
    method: MethodDescriptor<Req, Resp>,
    request: Req,
    callOptions: CallOptions = CallOptions.DEFAULT,
): Resp = suspendCancellableCoroutine { continuation ->
    val effectiveOptions = if (callOptions.deadline == null) {
        callOptions.withDeadlineAfter(20L, TimeUnit.SECONDS)
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

        override fun onCompleted() {
            // Unary response is delivered in onNext
        }
    })
}

internal object GetProfileRequestMarshaller : MethodDescriptor.Marshaller<GetProfileRequest> {
    override fun stream(value: GetProfileRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.userId.isNotEmpty()) {
            cos.writeString(1, value.userId)
        }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetProfileRequest {
        val cis = CodedInputStream.newInstance(stream)
        var userId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> userId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GetProfileRequest(userId = userId)
    }
}

internal object UpdateProfileRequestMarshaller : MethodDescriptor.Marshaller<UpdateProfileRequest> {
    override fun stream(value: UpdateProfileRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.userId.isNotEmpty()) {
            cos.writeString(1, value.userId)
        }
        if (value.heightCm != 0) {
            cos.writeInt32(2, value.heightCm)
        }
        if (value.weightKg != 0.0) {
            cos.writeDouble(3, value.weightKg)
        }
        if (value.gender.value > 0) {
            cos.writeEnum(4, value.gender.value)
        }
        if (value.activityLevel.value > 0) {
            cos.writeEnum(5, value.activityLevel.value)
        }
        if (value.goalType.value > 0) {
            cos.writeEnum(6, value.goalType.value)
        }
        val goalBytes = serializeNutritionGoal(value.nutritionGoal)
        cos.writeByteArray(7, goalBytes)
        if (value.weeklyBudget != 0.0) {
            cos.writeDouble(8, value.weeklyBudget)
        }
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): UpdateProfileRequest {
        val cis = CodedInputStream.newInstance(stream)
        var userId = ""
        var heightCm = 0
        var weightKg = 0.0
        var gender = GenderProto.GENDER_UNSPECIFIED
        var activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED
        var goalType = GoalTypeProto.GOAL_TYPE_UNSPECIFIED
        var nutritionGoal = NutritionGoalProto()
        var weeklyBudget = 0.0

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> userId = cis.readString()
                (2 shl 3) or 0 -> heightCm = cis.readInt32()
                (3 shl 3) or 1 -> weightKg = cis.readDouble()
                (4 shl 3) or 0 -> gender = GenderProto.fromValue(cis.readEnum())
                (5 shl 3) or 0 -> activityLevel = ActivityLevelProto.fromValue(cis.readEnum())
                (6 shl 3) or 0 -> goalType = GoalTypeProto.fromValue(cis.readEnum())
                (7 shl 3) or 2 -> {
                    val length = cis.readRawVarint32()
                    val oldLimit = cis.pushLimit(length)
                    nutritionGoal = parseNutritionGoal(cis)
                    cis.popLimit(oldLimit)
                }
                (8 shl 3) or 1 -> weeklyBudget = cis.readDouble()
                else -> cis.skipField(tag)
            }
        }

        return UpdateProfileRequest(
            userId = userId,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender,
            activityLevel = activityLevel,
            goalType = goalType,
            nutritionGoal = nutritionGoal,
            weeklyBudget = weeklyBudget,
        )
    }
}

internal object ProfileProtoMarshaller : MethodDescriptor.Marshaller<ProfileProto> {
    override fun stream(value: ProfileProto): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.userId.isNotEmpty()) cos.writeString(2, value.userId)
        if (value.heightCm != 0) cos.writeInt32(3, value.heightCm)
        if (value.weightKg != 0.0) cos.writeDouble(4, value.weightKg)
        if (value.gender.value > 0) cos.writeEnum(5, value.gender.value)
        if (value.activityLevel.value > 0) cos.writeEnum(6, value.activityLevel.value)
        if (value.goalType.value > 0) cos.writeEnum(7, value.goalType.value)
        val goalBytes = serializeNutritionGoal(value.nutritionGoal)
        cos.writeByteArray(8, goalBytes)
        if (value.updatedAt.seconds != 0L || value.updatedAt.nanos != 0) {
            val tbaos = ByteArrayOutputStream()
            val tcos = CodedOutputStream.newInstance(tbaos)
            if (value.updatedAt.seconds != 0L) tcos.writeInt64(1, value.updatedAt.seconds)
            if (value.updatedAt.nanos != 0) tcos.writeInt32(2, value.updatedAt.nanos)
            tcos.flush()
            cos.writeByteArray(9, tbaos.toByteArray())
        }
        if (value.weeklyBudget != 0.0) cos.writeDouble(10, value.weeklyBudget)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): ProfileProto {
        val cis = CodedInputStream.newInstance(stream)
        var id = ""
        var userId = ""
        var heightCm = 0
        var weightKg = 0.0
        var gender = GenderProto.GENDER_UNSPECIFIED
        var activityLevel = ActivityLevelProto.ACTIVITY_LEVEL_UNSPECIFIED
        var goalType = GoalTypeProto.GOAL_TYPE_UNSPECIFIED
        var nutritionGoal = NutritionGoalProto()
        var updatedAt = TimestampProto()
        var weeklyBudget = 0.0

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 2 -> userId = cis.readString()
                (3 shl 3) or 0 -> heightCm = cis.readInt32()
                (4 shl 3) or 1 -> weightKg = cis.readDouble()
                (5 shl 3) or 0 -> gender = GenderProto.fromValue(cis.readEnum())
                (6 shl 3) or 0 -> activityLevel = ActivityLevelProto.fromValue(cis.readEnum())
                (7 shl 3) or 0 -> goalType = GoalTypeProto.fromValue(cis.readEnum())
                (8 shl 3) or 2 -> {
                    val length = cis.readRawVarint32()
                    val oldLimit = cis.pushLimit(length)
                    nutritionGoal = parseNutritionGoal(cis)
                    cis.popLimit(oldLimit)
                }
                (9 shl 3) or 2 -> {
                    val length = cis.readRawVarint32()
                    val oldLimit = cis.pushLimit(length)
                    updatedAt = parseTimestamp(cis)
                    cis.popLimit(oldLimit)
                }
                (10 shl 3) or 1 -> weeklyBudget = cis.readDouble()
                else -> cis.skipField(tag)
            }
        }

        return ProfileProto(
            id = id,
            userId = userId,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender,
            activityLevel = activityLevel,
            goalType = goalType,
            nutritionGoal = nutritionGoal,
            updatedAt = updatedAt,
            weeklyBudget = weeklyBudget,
        )
    }
}

private fun serializeNutritionGoal(goal: NutritionGoalProto): ByteArray {
    val baos = ByteArrayOutputStream()
    val cos = CodedOutputStream.newInstance(baos)
    if (goal.targetCalories != 0) {
        cos.writeInt32(1, goal.targetCalories)
    }
    if (goal.targetProteinsGrams != 0.0) {
        cos.writeDouble(2, goal.targetProteinsGrams)
    }
    if (goal.targetFatsGrams != 0.0) {
        cos.writeDouble(3, goal.targetFatsGrams)
    }
    if (goal.targetCarbsGrams != 0.0) {
        cos.writeDouble(4, goal.targetCarbsGrams)
    }
    if (goal.targetWeightKg != 0.0) {
        cos.writeDouble(5, goal.targetWeightKg)
    }
    cos.flush()
    return baos.toByteArray()
}

private fun parseNutritionGoal(cis: CodedInputStream): NutritionGoalProto {
    var targetCalories = 0
    var targetProteins = 0.0
    var targetFats = 0.0
    var targetCarbs = 0.0
    var targetWeight = 0.0
    while (true) {
        val tag = cis.readTag()
        if (tag == 0) break
        when (tag) {
            (1 shl 3) or 0 -> targetCalories = cis.readInt32()
            (2 shl 3) or 1 -> targetProteins = cis.readDouble()
            (3 shl 3) or 1 -> targetFats = cis.readDouble()
            (4 shl 3) or 1 -> targetCarbs = cis.readDouble()
            (5 shl 3) or 1 -> targetWeight = cis.readDouble()
            else -> cis.skipField(tag)
        }
    }
    return NutritionGoalProto(
        targetCalories = targetCalories,
        targetProteinsGrams = targetProteins,
        targetFatsGrams = targetFats,
        targetCarbsGrams = targetCarbs,
        targetWeightKg = targetWeight,
    )
}

private fun parseTimestamp(cis: CodedInputStream): TimestampProto {
    var seconds = 0L
    var nanos = 0
    while (true) {
        val tag = cis.readTag()
        if (tag == 0) break
        when (tag) {
            (1 shl 3) or 0 -> seconds = cis.readInt64()
            (2 shl 3) or 0 -> nanos = cis.readInt32()
            else -> cis.skipField(tag)
        }
    }
    return TimestampProto(seconds = seconds, nanos = nanos)
}
