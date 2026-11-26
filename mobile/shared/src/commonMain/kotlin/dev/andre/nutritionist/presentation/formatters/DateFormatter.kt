package dev.andre.nutritionist.presentation.formatters

import dev.andre.nutritionist.presentation.model.DayOfWeekUiModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Чистый KMP-форматтер дат, недель и времени на базе kotlinx-datetime.
 */
object DateFormatter {

    fun formatDayHeader(
        epochMs: Long,
        nowEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val targetDate = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val todayDate = Instant.fromEpochMilliseconds(nowEpochMs).toLocalDateTime(timeZone).date
        val yesterdayDate = todayDate.minus(1, DateTimeUnit.DAY)

        return when (targetDate) {
            todayDate -> "Сегодня, ${targetDate.day} ${monthGenitive(targetDate.month)}"
            yesterdayDate -> "Вчера"
            else -> "${shortDayOfWeek(targetDate.dayOfWeek)}, ${targetDate.day} ${monthGenitive(targetDate.month)}"
        }
    }

    fun formatTime(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val dateTime = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone)
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    fun dayRangeEpochMs(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Pair<Long, Long> {
        val date: LocalDate = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val start = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endExclusive = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        return start to (endExclusive - 1L)
    }

    fun weekRangeEpochMs(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Pair<Long, Long> {
        val selectedDate = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val monday = selectedDate.minus(dayOffsetFromMonday(selectedDate.dayOfWeek), DateTimeUnit.DAY)
        val nextMonday = monday.plus(7, DateTimeUnit.DAY)
        val start = monday.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endExclusive = nextMonday.atStartOfDayIn(timeZone).toEpochMilliseconds()
        return start to (endExclusive - 1L)
    }

    fun weekDaysForDate(
        selectedDateEpochMs: Long,
        nowEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
        datesWithMeals: Set<LocalDate> = emptySet(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<DayOfWeekUiModel> {
        val selectedDate = Instant.fromEpochMilliseconds(selectedDateEpochMs).toLocalDateTime(timeZone).date
        val todayDate = Instant.fromEpochMilliseconds(nowEpochMs).toLocalDateTime(timeZone).date
        val monday = selectedDate.minus(dayOffsetFromMonday(selectedDate.dayOfWeek), DateTimeUnit.DAY)

        return (0..6).map { offset ->
            val dayDate = monday.plus(offset, DateTimeUnit.DAY)
            val noonEpochMs = LocalDateTime(dayDate, LocalTime(12, 0)).toInstant(timeZone).toEpochMilliseconds()
            DayOfWeekUiModel(
                date = dayDate,
                dateEpochMs = noonEpochMs,
                dayOfWeekName = shortDayOfWeek(dayDate.dayOfWeek),
                dayOfMonth = dayDate.day.toString().padStart(2, '0'),
                isSelected = dayDate == selectedDate,
                isToday = dayDate == todayDate,
                hasLoggedMeals = datesWithMeals.contains(dayDate),
            )
        }
    }

    fun formatMonthYear(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        return "${monthNominative(date.month)} ${date.year}"
    }

    fun formatIsoWeekLabel(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val weekNumber = isoWeekNumber(date)
        return "НЕДЕЛЯ $weekNumber"
    }

    fun shiftDaysEpochMs(
        epochMs: Long,
        deltaDays: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long {
        val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val target = if (deltaDays >= 0) {
            date.plus(deltaDays, DateTimeUnit.DAY)
        } else {
            date.minus(-deltaDays, DateTimeUnit.DAY)
        }
        return LocalDateTime(target, LocalTime(12, 0)).toInstant(timeZone).toEpochMilliseconds()
    }

    internal fun isoWeekNumber(date: LocalDate): Int {
        val thursday = date.plus(3 - dayOffsetFromMonday(date.dayOfWeek), DateTimeUnit.DAY)
        val jan4 = LocalDate(thursday.year, Month.JANUARY, 4)
        val firstThursday = jan4.plus(3 - dayOffsetFromMonday(jan4.dayOfWeek), DateTimeUnit.DAY)
        val daysDiff = thursday.toEpochDays() - firstThursday.toEpochDays()
        return (daysDiff / 7L).toInt() + 1
    }

    private fun dayOffsetFromMonday(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }

    private fun shortDayOfWeek(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Пн"
        DayOfWeek.TUESDAY -> "Вт"
        DayOfWeek.WEDNESDAY -> "Ср"
        DayOfWeek.THURSDAY -> "Чт"
        DayOfWeek.FRIDAY -> "Пт"
        DayOfWeek.SATURDAY -> "Сб"
        DayOfWeek.SUNDAY -> "Вс"
    }

    private fun monthNominative(month: Month): String = when (month) {
        Month.JANUARY -> "Январь"
        Month.FEBRUARY -> "Февраль"
        Month.MARCH -> "Март"
        Month.APRIL -> "Апрель"
        Month.MAY -> "Май"
        Month.JUNE -> "Июнь"
        Month.JULY -> "Июль"
        Month.AUGUST -> "Август"
        Month.SEPTEMBER -> "Сентябрь"
        Month.OCTOBER -> "Октябрь"
        Month.NOVEMBER -> "Ноябрь"
        Month.DECEMBER -> "Декабрь"
    }

    private fun monthGenitive(month: Month): String = when (month) {
        Month.JANUARY -> "января"
        Month.FEBRUARY -> "февраля"
        Month.MARCH -> "марта"
        Month.APRIL -> "апреля"
        Month.MAY -> "мая"
        Month.JUNE -> "июня"
        Month.JULY -> "июля"
        Month.AUGUST -> "августа"
        Month.SEPTEMBER -> "сентября"
        Month.OCTOBER -> "октября"
        Month.NOVEMBER -> "ноября"
        Month.DECEMBER -> "декабря"
    }
}
