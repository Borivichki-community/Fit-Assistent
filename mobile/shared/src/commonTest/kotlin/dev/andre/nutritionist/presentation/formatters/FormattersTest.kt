package dev.andre.nutritionist.presentation.formatters

import dev.andre.nutritionist.domain.model.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class FormattersTest {

    @Test
    fun `MoneyFormatter formats whole rubles and kopecks with space thousands separator`() {
        assertEquals("0 ₽", MoneyFormatter.format(Money.ZERO))
        assertEquals("350 ₽", MoneyFormatter.format(Money.fromMinor(35_000L)))
        assertEquals("12,50 ₽", MoneyFormatter.format(Money.fromMinor(1_250L)))
        assertEquals("1 500 ₽", MoneyFormatter.format(Money.fromMinor(150_000L)))
        assertEquals("1 234 567,05 ₽", MoneyFormatter.format(Money.fromMinor(123_456_705L)))
    }

    @Test
    fun `MoneyFormatter formats negative amounts and other currencies`() {
        assertEquals("-150 ₽", MoneyFormatter.formatWithCurrency(-15_000L, "RUB"))
        assertEquals("-0,99 ₽", MoneyFormatter.formatWithCurrency(-99L, "RUB"))
        assertEquals("19,99 $", MoneyFormatter.formatWithCurrency(1_999L, "USD"))
        assertEquals("10 €", MoneyFormatter.formatWithCurrency(1_000L, "eur"))
        assertEquals("500 GBP", MoneyFormatter.formatWithCurrency(50_000L, "GBP"))
    }

    @Test
    fun `NutritionFormatter formats calories grams and percentages`() {
        assertEquals("2 150 ккал", NutritionFormatter.formatCalories(2150.4))
        assertEquals("0 ккал", NutritionFormatter.formatCalories(Double.NaN))
        assertEquals("2 150", NutritionFormatter.formatCaloriesNumber(2150.4))
        assertEquals("145 г", NutritionFormatter.formatGram(145.0))
        assertEquals("12,5 г", NutritionFormatter.formatGram(12.5))
        assertEquals("1 200 г", NutritionFormatter.formatGram(1200.0))
        assertEquals("1,25", NutritionFormatter.formatMacroValue(1.25))
        assertEquals("13,8", NutritionFormatter.formatMacroValue(13.8))
        assertEquals("4,93", NutritionFormatter.formatMacroValue(4.93))
        assertEquals("85", NutritionFormatter.formatMacroValue(85.0))
        assertEquals("74%", NutritionFormatter.formatPercentage(0.744))
        assertEquals("0%", NutritionFormatter.formatPercentage(-0.2))
        assertEquals("120%", NutritionFormatter.formatPercentage(1.2))
    }

    @Test
    fun `DateFormatter formats today yesterday past dates and time deterministically`() {
        val nowEpochMs = Instant.parse("2026-11-24T12:30:00Z").toEpochMilliseconds()
        val sameDayMs = Instant.parse("2026-11-24T08:05:00Z").toEpochMilliseconds()
        val yesterdayMs = Instant.parse("2026-11-23T20:00:00Z").toEpochMilliseconds()
        val mondayLastWeekMs = Instant.parse("2026-11-16T09:15:00Z").toEpochMilliseconds()

        assertEquals(
            "Сегодня, 24 ноября",
            DateFormatter.formatDayHeader(sameDayMs, nowEpochMs, TimeZone.UTC),
        )
        assertEquals(
            "Вчера",
            DateFormatter.formatDayHeader(yesterdayMs, nowEpochMs, TimeZone.UTC),
        )
        assertEquals(
            "Пн, 16 ноября",
            DateFormatter.formatDayHeader(mondayLastWeekMs, nowEpochMs, TimeZone.UTC),
        )
        assertEquals("08:05", DateFormatter.formatTime(sameDayMs, TimeZone.UTC))
    }

    @Test
    fun `DateFormatter dayRangeEpochMs returns inclusive start and end of UTC day`() {
        val midDayMs = Instant.parse("2026-11-24T14:22:10Z").toEpochMilliseconds()
        val (start, end) = DateFormatter.dayRangeEpochMs(midDayMs, TimeZone.UTC)

        assertEquals(Instant.parse("2026-11-24T00:00:00Z").toEpochMilliseconds(), start)
        assertEquals(Instant.parse("2026-11-24T23:59:59.999Z").toEpochMilliseconds(), end)
    }

    @Test
    fun `DateFormatter calendar helpers compute week days month year ISO week and shifting`() {
        val satNov7Ms = Instant.parse("2026-11-07T12:00:00Z").toEpochMilliseconds()
        assertEquals("Ноябрь 2026", DateFormatter.formatMonthYear(satNov7Ms, TimeZone.UTC))
        assertEquals("НЕДЕЛЯ 45", DateFormatter.formatIsoWeekLabel(satNov7Ms, TimeZone.UTC))

        val loggedDates = setOf(LocalDate(2026, 11, 6), LocalDate(2026, 11, 7))
        val weekDays = DateFormatter.weekDaysForDate(
            selectedDateEpochMs = satNov7Ms,
            nowEpochMs = satNov7Ms,
            datesWithMeals = loggedDates,
            timeZone = TimeZone.UTC,
        )

        assertEquals(7, weekDays.size)
        assertEquals("Пн", weekDays[0].dayOfWeekName)
        assertEquals("02", weekDays[0].dayOfMonth)
        assertFalse(weekDays[0].isSelected)
        assertFalse(weekDays[0].hasLoggedMeals)

        assertTrue(weekDays[4].hasLoggedMeals)
        assertEquals("Сб", weekDays[5].dayOfWeekName)
        assertEquals("07", weekDays[5].dayOfMonth)
        assertTrue(weekDays[5].isSelected)
        assertTrue(weekDays[5].isToday)
        assertTrue(weekDays[5].hasLoggedMeals)

        val prevWeekMs = DateFormatter.shiftDaysEpochMs(satNov7Ms, -7, TimeZone.UTC)
        assertEquals("НЕДЕЛЯ 44", DateFormatter.formatIsoWeekLabel(prevWeekMs, TimeZone.UTC))

        val nextWeekMs = DateFormatter.shiftDaysEpochMs(satNov7Ms, 7, TimeZone.UTC)
        assertEquals("НЕДЕЛЯ 46", DateFormatter.formatIsoWeekLabel(nextWeekMs, TimeZone.UTC))
    }
}
