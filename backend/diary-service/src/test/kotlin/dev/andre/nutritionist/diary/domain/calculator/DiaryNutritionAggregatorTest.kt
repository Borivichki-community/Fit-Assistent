package dev.andre.nutritionist.diary.domain.calculator

import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.FoodItemId
import dev.andre.nutritionist.diary.domain.model.MealType
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import dev.andre.nutritionist.diary.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Instant

class DiaryNutritionAggregatorTest {
    @Test
    fun `empty day contains four ordered zero totals`() {
        val day = DiaryNutritionAggregator.aggregate(emptyList())

        assertEquals(emptyList<DiaryEntry>(), day.entries)
        assertEquals(NutritionSummary(0, 0.0, 0.0, 0.0), day.dailyTotal)
        assertEquals(MealType.entries.toList(), day.mealTotals.map { it.mealType })
        day.mealTotals.forEach { assertEquals(day.dailyTotal, it.nutrition) }
    }

    @Test
    fun `groups saved portions without multiplying by quantity`() {
        val entries = listOf(entry(MealType.LUNCH), entry(MealType.BREAKFAST), entry(MealType.LUNCH))
        val day = DiaryNutritionAggregator.aggregate(entries)

        assertEquals(entries, day.entries)
        assertEquals(NutritionSummary(450, 0.3, 0.6, 0.9), day.dailyTotal)
        assertEquals(NutritionSummary(300, 0.2, 0.4, 0.6), day.mealTotals[1].nutrition)
        assertEquals(NutritionSummary(0, 0.0, 0.0, 0.0), day.mealTotals[2].nutrition)
    }

    @Test
    fun `all meal types contribute to daily total`() {
        val day = DiaryNutritionAggregator.aggregate(MealType.entries.map { entry(it) })

        assertEquals(NutritionSummary(600, 0.4, 0.8, 1.2), day.dailyTotal)
        day.mealTotals.forEach { assertEquals(NutritionSummary(150, 0.1, 0.2, 0.3), it.nutrition) }
    }

    @Test
    fun `hundredths do not accumulate binary rounding errors`() {
        val day = DiaryNutritionAggregator.aggregate(List(100) { entry(MealType.SNACK) })

        assertEquals(NutritionSummary(15000, 10.0, 20.0, 30.0), day.dailyTotal)
        assertEquals(day.dailyTotal, day.mealTotals.last().nutrition)
    }

    @Test
    fun `maximum calories are preserved and overflow is rejected across groups`() {
        val maximum = entry(MealType.BREAKFAST).copy(nutrition = NutritionSummary(Int.MAX_VALUE, 0.0, 0.0, 0.0))
        assertEquals(Int.MAX_VALUE, DiaryNutritionAggregator.aggregate(listOf(maximum)).dailyTotal.calories)

        assertThrows(ArithmeticException::class.java) {
            DiaryNutritionAggregator.aggregate(listOf(maximum, entry(MealType.LUNCH)))
        }
    }

    private fun entry(mealType: MealType): DiaryEntry =
        DiaryEntry(
            id = DiaryEntryId(UUID.randomUUID()),
            userId = UserId("00000000-0000-0000-0000-000000000001"),
            foodItemId = FoodItemId(UUID.randomUUID()),
            foodName = "Snapshot",
            quantityGrams = 800.0,
            nutrition = NutritionSummary(150, 0.1, 0.2, 0.3),
            mealType = mealType,
            recordedAt = Instant.parse("2026-11-17T12:00:00Z"),
            createdAt = Instant.parse("2026-11-17T12:00:00Z"),
        )
}
