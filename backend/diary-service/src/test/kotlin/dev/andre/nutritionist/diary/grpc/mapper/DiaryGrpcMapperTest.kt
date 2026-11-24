package dev.andre.nutritionist.diary.grpc.mapper

import com.budgetntr.proto.diary.v1.AddEntryRequest
import com.google.protobuf.Timestamp
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.EntrySourceType
import dev.andre.nutritionist.diary.domain.model.IdempotencyKey
import dev.andre.nutritionist.diary.domain.model.MealNutritionSummary
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import dev.andre.nutritionist.diary.domain.model.SourceId
import dev.andre.nutritionist.diary.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.DateTimeException
import java.util.UUID
import kotlin.time.Instant
import com.budgetntr.proto.diary.v1.EntrySourceType as EntrySourceTypeProto
import com.budgetntr.proto.diary.v1.NutritionSummary as NutritionSummaryProto

class DiaryGrpcMapperTest {
    private val mapper = DiaryGrpcMapper()

    @Test
    fun `meal summary maps all nutrition fields`() {
        val proto = mapper.toProto(MealNutritionSummary(MealType.DINNER, NutritionSummary(450, 12.34, 5.67, 8.9)))

        assertEquals("DINNER", proto.mealType)
        assertEquals(450, proto.nutrition.calories)
        assertEquals(12.34, proto.nutrition.proteins)
        assertEquals(5.67, proto.nutrition.fats)
        assertEquals(8.9, proto.nutrition.carbs)
    }

    @Test
    fun `add entry request maps snapshot and extensible source type`() {
        val entryId = UUID.randomUUID()
        val sourceId = UUID.randomUUID()
        val request =
            AddEntryRequest
                .newBuilder()
                .setEntryId(entryId.toString())
                .setMealType("LUNCH")
                .setRecordedAt(Timestamp.newBuilder().setSeconds(1_700_000_000).build())
                .setSourceType(EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT)
                .setSourceId(sourceId.toString())
                .setNameSnapshot("Овсянка с ягодами")
                .setWeightGrams(250.0)
                .setNutrition(
                    NutritionSummaryProto
                        .newBuilder()
                        .setCalories(320)
                        .setProteins(10.0)
                        .setFats(5.0)
                        .setCarbs(55.0)
                        .build(),
                ).build()

        val command = mapper.toCommand(request, UserId("user-1"), IdempotencyKey("key-1"))

        assertEquals(entryId, command.entryId.raw)
        assertEquals(EntrySourceType.PRODUCT, command.sourceType)
        assertEquals(sourceId, command.sourceId?.raw)
        assertEquals("Овсянка с ягодами", command.nameSnapshot)
        assertEquals(250.0, command.weightGrams)
        assertEquals(320, command.nutrition.calories)
    }

    @Test
    fun `custom entry without source id maps correctly`() {
        val entryId = UUID.randomUUID()
        val request =
            AddEntryRequest
                .newBuilder()
                .setEntryId(entryId.toString())
                .setMealType("SNACK")
                .setRecordedAt(Timestamp.newBuilder().setSeconds(1_700_000_000).build())
                .setSourceType(EntrySourceTypeProto.ENTRY_SOURCE_TYPE_CUSTOM)
                .setNameSnapshot("Домашний сэндвич")
                .setWeightGrams(180.0)
                .setNutrition(
                    NutritionSummaryProto
                        .newBuilder()
                        .setCalories(280)
                        .setProteins(8.0)
                        .setFats(12.0)
                        .setCarbs(32.0)
                        .build(),
                ).build()

        val command = mapper.toCommand(request, UserId("user-1"), IdempotencyKey("key-2"))

        assertEquals(EntrySourceType.CUSTOM, command.sourceType)
        assertNull(command.sourceId)
        assertEquals("Домашний сэндвич", command.nameSnapshot)
    }

    @Test
    fun `toProto maps diary entry to DiaryEntryItem snapshot`() {
        val entryId = DiaryEntryId(UUID.randomUUID())
        val sourceId = SourceId(UUID.randomUUID())
        val entry =
            DiaryEntry(
                id = entryId,
                userId = UserId("user-1"),
                sourceType = EntrySourceType.PRODUCT,
                sourceId = sourceId,
                nameSnapshot = "Гречневая каша",
                weightGrams = 200.0,
                nutrition = NutritionSummary(220, 8.0, 2.5, 42.0),
                mealType = MealType.BREAKFAST,
                recordedAt = Instant.fromEpochSeconds(1_700_000_000, 0),
                createdAt = Instant.fromEpochSeconds(1_700_000_000, 0),
            )

        val item = mapper.toProto(entry)

        assertEquals(entryId.raw.toString(), item.id)
        assertEquals(EntrySourceTypeProto.ENTRY_SOURCE_TYPE_PRODUCT, item.sourceType)
        assertEquals(sourceId.raw.toString(), item.sourceId)
        assertEquals("Гречневая каша", item.nameSnapshot)
        assertEquals(200.0, item.weightGrams)
        assertEquals("Гречневая каша", item.foodName)
        assertEquals(sourceId.raw.toString(), item.foodItemId)
    }

    @Test
    fun `local days honor both daylight saving transitions`() {
        val spring = mapper.dateBounds("2026-03-29", "2026-03-29", "Europe/Berlin")
        val autumn = mapper.dateBounds("2026-10-25", "2026-10-25", "Europe/Berlin")

        assertEquals(Instant.parse("2026-03-28T23:00:00Z"), spring.first)
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), spring.second)
        assertEquals(23 * 3600L, spring.second.epochSeconds - spring.first.epochSeconds)
        assertEquals(Instant.parse("2026-10-24T22:00:00Z"), autumn.first)
        assertEquals(Instant.parse("2026-10-25T23:00:00Z"), autumn.second)
        assertEquals(25 * 3600L, autumn.second.epochSeconds - autumn.first.epochSeconds)
    }

    @Test
    fun `invalid dates zones and reversed ranges fail`() {
        assertThrows(DateTimeException::class.java) { mapper.dateBounds("2026-02-30", "2026-02-30", "UTC") }
        assertThrows(DateTimeException::class.java) { mapper.dateBounds("2026-11-17", "2026-11-17", "Invalid/Zone") }
        assertThrows(IllegalArgumentException::class.java) { mapper.dateBounds("2026-11-18", "2026-11-17", "UTC") }
    }
}
