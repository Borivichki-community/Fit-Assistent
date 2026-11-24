package dev.andre.nutritionist.data.remote.grpc

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.AddEntryResponse
import com.budgetntr.proto.diary.v1.CostSnapshotProto
import com.budgetntr.proto.diary.v1.DeleteMealRequest
import com.budgetntr.proto.diary.v1.DeleteMealResponse
import com.budgetntr.proto.diary.v1.DiaryEntryItemProto
import com.budgetntr.proto.diary.v1.EntrySourceTypeProto
import com.budgetntr.proto.diary.v1.GetDailyEntriesRequest
import com.budgetntr.proto.diary.v1.GetDailyEntriesResponse
import com.budgetntr.proto.diary.v1.MealNutritionSummaryProto
import com.budgetntr.proto.diary.v1.MoneyProto
import com.budgetntr.proto.diary.v1.NutritionSummaryProto
import com.budgetntr.proto.profile.v1.TimestampProto
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import io.grpc.Channel
import io.grpc.MethodDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AndroidDiaryGrpcServiceClient(
    private val channel: Channel,
) : DiaryGrpcServiceClient {

    override suspend fun addEntry(request: AddEntryRequest): AddEntryResponse {
        return channel.unaryCall(addEntryMethod, request)
    }

    override suspend fun getDailyEntries(request: GetDailyEntriesRequest): GetDailyEntriesResponse {
        return channel.unaryCall(getDailyEntriesMethod, request)
    }

    override suspend fun deleteMeal(request: DeleteMealRequest): DeleteMealResponse {
        return channel.unaryCall(deleteMealMethod, request)
    }

    companion object {
        private const val SERVICE_NAME = "com.budgetntr.diary.v1.DiaryService"

        val addEntryMethod: MethodDescriptor<AddEntryRequest, AddEntryResponse> =
            MethodDescriptor.newBuilder<AddEntryRequest, AddEntryResponse>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "AddEntry"))
                .setRequestMarshaller(AddEntryRequestMarshaller)
                .setResponseMarshaller(AddEntryResponseMarshaller)
                .build()

        val getDailyEntriesMethod: MethodDescriptor<GetDailyEntriesRequest, GetDailyEntriesResponse> =
            MethodDescriptor.newBuilder<GetDailyEntriesRequest, GetDailyEntriesResponse>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "GetDailyEntries"))
                .setRequestMarshaller(GetDailyEntriesRequestMarshaller)
                .setResponseMarshaller(GetDailyEntriesResponseMarshaller)
                .build()

        val deleteMealMethod: MethodDescriptor<DeleteMealRequest, DeleteMealResponse> =
            MethodDescriptor.newBuilder<DeleteMealRequest, DeleteMealResponse>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "DeleteMeal"))
                .setRequestMarshaller(DeleteMealRequestMarshaller)
                .setResponseMarshaller(DeleteMealResponseMarshaller)
                .build()
    }
}

internal object AddEntryRequestMarshaller : MethodDescriptor.Marshaller<AddEntryRequest> {
    override fun stream(value: AddEntryRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.entryId.isNotEmpty()) cos.writeString(1, value.entryId)
        if (value.mealType.isNotEmpty()) cos.writeString(2, value.mealType)

        // recorded_at: 3
        val tsBaos = ByteArrayOutputStream()
        val tsCos = CodedOutputStream.newInstance(tsBaos)
        if (value.recordedAt.seconds != 0L) tsCos.writeInt64(1, value.recordedAt.seconds)
        if (value.recordedAt.nanos != 0) tsCos.writeInt32(2, value.recordedAt.nanos)
        tsCos.flush()
        cos.writeByteArray(3, tsBaos.toByteArray())

        if (value.sourceType != EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED) {
            cos.writeInt32(4, value.sourceType.value)
        }
        value.sourceId?.let { cos.writeString(5, it) }
        if (value.nameSnapshot.isNotEmpty()) cos.writeString(6, value.nameSnapshot)
        if (value.weightGrams != 0.0) cos.writeDouble(7, value.weightGrams)

        // nutrition: 8
        val nutBaos = ByteArrayOutputStream()
        val nutCos = CodedOutputStream.newInstance(nutBaos)
        if (value.nutrition.calories != 0) nutCos.writeInt32(1, value.nutrition.calories)
        if (value.nutrition.proteins != 0.0) nutCos.writeDouble(2, value.nutrition.proteins)
        if (value.nutrition.fats != 0.0) nutCos.writeDouble(3, value.nutrition.fats)
        if (value.nutrition.carbs != 0.0) nutCos.writeDouble(4, value.nutrition.carbs)
        nutCos.flush()
        cos.writeByteArray(8, nutBaos.toByteArray())

        // cost: 9
        val costBaos = ByteArrayOutputStream()
        val costCos = CodedOutputStream.newInstance(costBaos)
        // total: 1
        val totBaos = ByteArrayOutputStream()
        val totCos = CodedOutputStream.newInstance(totBaos)
        if (value.cost.total.amountMinor != 0L) totCos.writeInt64(1, value.cost.total.amountMinor)
        if (value.cost.total.currencyCode.isNotEmpty()) totCos.writeString(2, value.cost.total.currencyCode)
        totCos.flush()
        costCos.writeByteArray(1, totBaos.toByteArray())
        costCos.flush()
        cos.writeByteArray(9, costBaos.toByteArray())

        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): AddEntryRequest {
        val cis = CodedInputStream.newInstance(stream)
        var entryId = ""
        var mealType = ""
        var recordedAt = TimestampProto()
        var sourceType = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED
        var sourceId: String? = null
        var nameSnapshot = ""
        var weightGrams = 0.0
        var nutrition = NutritionSummaryProto()
        var cost = CostSnapshotProto()

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> entryId = cis.readString()
                (2 shl 3) or 2 -> mealType = cis.readString()
                (3 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var sec = 0L
                    var nanos = 0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 0 -> sec = c.readInt64()
                            (2 shl 3) or 0 -> nanos = c.readInt32()
                            else -> c.skipField(t)
                        }
                    }
                    recordedAt = TimestampProto(sec, nanos)
                }
                (4 shl 3) or 0 -> sourceType = EntrySourceTypeProto.fromValue(cis.readInt32())
                (5 shl 3) or 2 -> sourceId = cis.readString()
                (6 shl 3) or 2 -> nameSnapshot = cis.readString()
                (7 shl 3) or 1 -> weightGrams = cis.readDouble()
                (8 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var cal = 0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 0 -> cal = c.readInt32()
                            (2 shl 3) or 1 -> p = c.readDouble()
                            (3 shl 3) or 1 -> f = c.readDouble()
                            (4 shl 3) or 1 -> cb = c.readDouble()
                            else -> c.skipField(t)
                        }
                    }
                    nutrition = NutritionSummaryProto(cal, p, f, cb)
                }
                (9 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var total = MoneyProto()
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 2 -> {
                                val mb = c.readByteArray()
                                val mc = CodedInputStream.newInstance(mb)
                                var am = 0L
                                var curr = "RUB"
                                while (true) {
                                    val mt = mc.readTag()
                                    if (mt == 0) break
                                    when (mt) {
                                        (1 shl 3) or 0 -> am = mc.readInt64()
                                        (2 shl 3) or 2 -> curr = mc.readString()
                                        else -> mc.skipField(mt)
                                    }
                                }
                                total = MoneyProto(am, curr)
                            }
                            else -> c.skipField(t)
                        }
                    }
                    cost = CostSnapshotProto(total = total)
                }
                else -> cis.skipField(tag)
            }
        }
        return AddEntryRequest(
            entryId = entryId,
            mealType = mealType,
            recordedAt = recordedAt,
            sourceType = sourceType,
            sourceId = sourceId,
            nameSnapshot = nameSnapshot,
            weightGrams = weightGrams,
            nutrition = nutrition,
            cost = cost,
        )
    }
}

internal object AddEntryResponseMarshaller : MethodDescriptor.Marshaller<AddEntryResponse> {
    override fun stream(value: AddEntryResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        val entryBytes = DiaryItemMarshallerHelper.toByteArray(value.entry)
        cos.writeByteArray(1, entryBytes)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): AddEntryResponse {
        val cis = CodedInputStream.newInstance(stream)
        var entry = DiaryEntryItemProto()
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> entry = DiaryItemMarshallerHelper.fromByteArray(cis.readByteArray())
                else -> cis.skipField(tag)
            }
        }
        return AddEntryResponse(entry = entry)
    }
}

internal object GetDailyEntriesRequestMarshaller : MethodDescriptor.Marshaller<GetDailyEntriesRequest> {
    override fun stream(value: GetDailyEntriesRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.date.isNotEmpty()) cos.writeString(1, value.date)
        if (value.timeZone.isNotEmpty()) cos.writeString(2, value.timeZone)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetDailyEntriesRequest {
        val cis = CodedInputStream.newInstance(stream)
        var date = ""
        var timeZone = "UTC"
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> date = cis.readString()
                (2 shl 3) or 2 -> timeZone = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return GetDailyEntriesRequest(date = date, timeZone = timeZone)
    }
}

internal object GetDailyEntriesResponseMarshaller : MethodDescriptor.Marshaller<GetDailyEntriesResponse> {
    override fun stream(value: GetDailyEntriesResponse): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        for (entry in value.entries) {
            cos.writeByteArray(1, DiaryItemMarshallerHelper.toByteArray(entry))
        }
        // daily_total: 2
        val dtBaos = ByteArrayOutputStream()
        val dtCos = CodedOutputStream.newInstance(dtBaos)
        if (value.dailyTotal.calories != 0) dtCos.writeInt32(1, value.dailyTotal.calories)
        if (value.dailyTotal.proteins != 0.0) dtCos.writeDouble(2, value.dailyTotal.proteins)
        if (value.dailyTotal.fats != 0.0) dtCos.writeDouble(3, value.dailyTotal.fats)
        if (value.dailyTotal.carbs != 0.0) dtCos.writeDouble(4, value.dailyTotal.carbs)
        dtCos.flush()
        cos.writeByteArray(2, dtBaos.toByteArray())

        // meal_totals: 3 (repeated)
        for (mt in value.mealTotals) {
            val mtBaos = ByteArrayOutputStream()
            val mtCos = CodedOutputStream.newInstance(mtBaos)
            if (mt.mealType.isNotEmpty()) mtCos.writeString(1, mt.mealType)
            val nBaos = ByteArrayOutputStream()
            val nCos = CodedOutputStream.newInstance(nBaos)
            if (mt.nutrition.calories != 0) nCos.writeInt32(1, mt.nutrition.calories)
            if (mt.nutrition.proteins != 0.0) nCos.writeDouble(2, mt.nutrition.proteins)
            if (mt.nutrition.fats != 0.0) nCos.writeDouble(3, mt.nutrition.fats)
            if (mt.nutrition.carbs != 0.0) nCos.writeDouble(4, mt.nutrition.carbs)
            nCos.flush()
            mtCos.writeByteArray(2, nBaos.toByteArray())
            mtCos.flush()
            cos.writeByteArray(3, mtBaos.toByteArray())
        }

        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): GetDailyEntriesResponse {
        val cis = CodedInputStream.newInstance(stream)
        val entries = mutableListOf<DiaryEntryItemProto>()
        var dailyTotal = NutritionSummaryProto()
        val mealTotals = mutableListOf<MealNutritionSummaryProto>()

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> entries.add(DiaryItemMarshallerHelper.fromByteArray(cis.readByteArray()))
                (2 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var cal = 0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 0 -> cal = c.readInt32()
                            (2 shl 3) or 1 -> p = c.readDouble()
                            (3 shl 3) or 1 -> f = c.readDouble()
                            (4 shl 3) or 1 -> cb = c.readDouble()
                            else -> c.skipField(t)
                        }
                    }
                    dailyTotal = NutritionSummaryProto(cal, p, f, cb)
                }
                (3 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var mType = ""
                    var nut = NutritionSummaryProto()
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 2 -> mType = c.readString()
                            (2 shl 3) or 2 -> {
                                val nb = c.readByteArray()
                                val nc = CodedInputStream.newInstance(nb)
                                var cal = 0
                                var p = 0.0
                                var f = 0.0
                                var cb = 0.0
                                while (true) {
                                    val nt = nc.readTag()
                                    if (nt == 0) break
                                    when (nt) {
                                        (1 shl 3) or 0 -> cal = nc.readInt32()
                                        (2 shl 3) or 1 -> p = nc.readDouble()
                                        (3 shl 3) or 1 -> f = nc.readDouble()
                                        (4 shl 3) or 1 -> cb = nc.readDouble()
                                        else -> nc.skipField(nt)
                                    }
                                }
                                nut = NutritionSummaryProto(cal, p, f, cb)
                            }
                            else -> c.skipField(t)
                        }
                    }
                    mealTotals.add(MealNutritionSummaryProto(mType, nut))
                }
                else -> cis.skipField(tag)
            }
        }
        return GetDailyEntriesResponse(entries = entries, dailyTotal = dailyTotal, mealTotals = mealTotals)
    }
}

internal object DeleteMealRequestMarshaller : MethodDescriptor.Marshaller<DeleteMealRequest> {
    override fun stream(value: DeleteMealRequest): InputStream {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.entryId.isNotEmpty()) cos.writeString(1, value.entryId)
        cos.flush()
        return ByteArrayInputStream(baos.toByteArray())
    }

    override fun parse(stream: InputStream): DeleteMealRequest {
        val cis = CodedInputStream.newInstance(stream)
        var entryId = ""
        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> entryId = cis.readString()
                else -> cis.skipField(tag)
            }
        }
        return DeleteMealRequest(entryId = entryId)
    }
}

internal object DeleteMealResponseMarshaller : MethodDescriptor.Marshaller<DeleteMealResponse> {
    override fun stream(value: DeleteMealResponse): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun parse(stream: InputStream): DeleteMealResponse = DeleteMealResponse(true)
}

internal object DiaryItemMarshallerHelper {
    fun toByteArray(value: DiaryEntryItemProto): ByteArray {
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        if (value.id.isNotEmpty()) cos.writeString(1, value.id)
        if (value.sourceType != EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED) {
            cos.writeInt32(2, value.sourceType.value)
        }
        value.sourceId?.let { cos.writeString(3, it) }
        if (value.nameSnapshot.isNotEmpty()) cos.writeString(4, value.nameSnapshot)
        if (value.weightGrams != 0.0) cos.writeDouble(5, value.weightGrams)

        // nutrition: 6
        val nBaos = ByteArrayOutputStream()
        val nCos = CodedOutputStream.newInstance(nBaos)
        if (value.nutrition.calories != 0) nCos.writeInt32(1, value.nutrition.calories)
        if (value.nutrition.proteins != 0.0) nCos.writeDouble(2, value.nutrition.proteins)
        if (value.nutrition.fats != 0.0) nCos.writeDouble(3, value.nutrition.fats)
        if (value.nutrition.carbs != 0.0) nCos.writeDouble(4, value.nutrition.carbs)
        nCos.flush()
        cos.writeByteArray(6, nBaos.toByteArray())

        // recorded_at: 7
        val tBaos = ByteArrayOutputStream()
        val tCos = CodedOutputStream.newInstance(tBaos)
        if (value.recordedAt.seconds != 0L) tCos.writeInt64(1, value.recordedAt.seconds)
        if (value.recordedAt.nanos != 0) tCos.writeInt32(2, value.recordedAt.nanos)
        tCos.flush()
        cos.writeByteArray(7, tBaos.toByteArray())

        if (value.mealType.isNotEmpty()) cos.writeString(8, value.mealType)

        // cost: 9
        val cBaos = ByteArrayOutputStream()
        val cCos = CodedOutputStream.newInstance(cBaos)
        val totBaos = ByteArrayOutputStream()
        val totCos = CodedOutputStream.newInstance(totBaos)
        if (value.cost.total.amountMinor != 0L) totCos.writeInt64(1, value.cost.total.amountMinor)
        if (value.cost.total.currencyCode.isNotEmpty()) totCos.writeString(2, value.cost.total.currencyCode)
        totCos.flush()
        cCos.writeByteArray(1, totBaos.toByteArray())
        cCos.flush()
        cos.writeByteArray(9, cBaos.toByteArray())

        cos.flush()
        return baos.toByteArray()
    }

    fun fromByteArray(bytes: ByteArray): DiaryEntryItemProto {
        val cis = CodedInputStream.newInstance(bytes)
        var id = ""
        var sourceType = EntrySourceTypeProto.ENTRY_SOURCE_TYPE_UNSPECIFIED
        var sourceId: String? = null
        var nameSnapshot = ""
        var weightGrams = 0.0
        var nutrition = NutritionSummaryProto()
        var recordedAt = TimestampProto()
        var mealType = ""
        var cost = CostSnapshotProto()

        while (true) {
            val tag = cis.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or 2 -> id = cis.readString()
                (2 shl 3) or 0 -> sourceType = EntrySourceTypeProto.fromValue(cis.readInt32())
                (3 shl 3) or 2 -> sourceId = cis.readString()
                (4 shl 3) or 2 -> nameSnapshot = cis.readString()
                (5 shl 3) or 1 -> weightGrams = cis.readDouble()
                (6 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var cal = 0
                    var p = 0.0
                    var f = 0.0
                    var cb = 0.0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 0 -> cal = c.readInt32()
                            (2 shl 3) or 1 -> p = c.readDouble()
                            (3 shl 3) or 1 -> f = c.readDouble()
                            (4 shl 3) or 1 -> cb = c.readDouble()
                            else -> c.skipField(t)
                        }
                    }
                    nutrition = NutritionSummaryProto(cal, p, f, cb)
                }
                (7 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var sec = 0L
                    var nanos = 0
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 0 -> sec = c.readInt64()
                            (2 shl 3) or 0 -> nanos = c.readInt32()
                            else -> c.skipField(t)
                        }
                    }
                    recordedAt = TimestampProto(sec, nanos)
                }
                (8 shl 3) or 2 -> mealType = cis.readString()
                (9 shl 3) or 2 -> {
                    val b = cis.readByteArray()
                    val c = CodedInputStream.newInstance(b)
                    var total = MoneyProto()
                    while (true) {
                        val t = c.readTag()
                        if (t == 0) break
                        when (t) {
                            (1 shl 3) or 2 -> {
                                val mb = c.readByteArray()
                                val mc = CodedInputStream.newInstance(mb)
                                var am = 0L
                                var curr = "RUB"
                                while (true) {
                                    val mt = mc.readTag()
                                    if (mt == 0) break
                                    when (mt) {
                                        (1 shl 3) or 0 -> am = mc.readInt64()
                                        (2 shl 3) or 2 -> curr = mc.readString()
                                        else -> mc.skipField(mt)
                                    }
                                }
                                total = MoneyProto(am, curr)
                            }
                            else -> c.skipField(t)
                        }
                    }
                    cost = CostSnapshotProto(total = total)
                }
                else -> cis.skipField(tag)
            }
        }
        return DiaryEntryItemProto(
            id = id,
            sourceType = sourceType,
            sourceId = sourceId,
            nameSnapshot = nameSnapshot,
            weightGrams = weightGrams,
            nutrition = nutrition,
            recordedAt = recordedAt,
            mealType = mealType,
            cost = cost,
        )
    }
}
