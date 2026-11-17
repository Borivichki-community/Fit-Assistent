package dev.andre.nutritionist.diary.grpc.mapper

import dev.andre.nutritionist.diary.domain.model.MealNutritionSummary
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.DateTimeException
import kotlin.time.Instant

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
