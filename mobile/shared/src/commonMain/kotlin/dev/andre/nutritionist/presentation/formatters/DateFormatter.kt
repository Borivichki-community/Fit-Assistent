package dev.andre.nutritionist.presentation.formatters

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Чистый KMP-форматтер дат и времени на базе kotlinx-datetime.
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

    private fun shortDayOfWeek(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Пн"
        DayOfWeek.TUESDAY -> "Вт"
        DayOfWeek.WEDNESDAY -> "Ср"
        DayOfWeek.THURSDAY -> "Чт"
        DayOfWeek.FRIDAY -> "Пт"
        DayOfWeek.SATURDAY -> "Сб"
        DayOfWeek.SUNDAY -> "Вс"
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
