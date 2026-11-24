package dev.andre.nutritionist.android.servicecheck

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.budgetntr.proto.diary.v1.NutritionSummary
import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

internal data class DiaryCheckEntry(
    val request: AddEntryRequest,
    val date: LocalDate,
    val addKey: String,
    val deleteKey: String,
    val deleted: Boolean = false,
) {
    companion object {
        fun create(now: Instant): DiaryCheckEntry {
            val request = AddEntryRequest.newBuilder()
                .setEntryId(UUID.randomUUID().toString())
                .setFoodItemId(UUID.randomUUID().toString())
                .setFoodName("Android debug test entry")
                .setQuantityGrams(100.0)
                .setRecordedAt(Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano))
                .setMealType("BREAKFAST")
                .setNutrition(
                    NutritionSummary.newBuilder()
                        .setCalories(52)
                        .setProteins(0.3)
                        .setFats(0.2)
                        .setCarbs(13.8),
                )
                .build()
            return DiaryCheckEntry(
                request = request,
                date = now.atOffset(ZoneOffset.UTC).toLocalDate(),
                addKey = UUID.randomUUID().toString(),
                deleteKey = UUID.randomUUID().toString(),
            )
        }
    }
}
