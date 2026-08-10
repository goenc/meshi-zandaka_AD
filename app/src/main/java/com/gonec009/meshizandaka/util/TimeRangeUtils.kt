package com.gonec009.meshizandaka.util

import com.gonec009.meshizandaka.domain.model.WeekStartDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TimeRangeUtils {
    fun todayRange(nowMillis: Long, zoneId: ZoneId): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return dateRange(date, zoneId)
    }

    fun weekRange(nowMillis: Long, zoneId: ZoneId, weekStartDay: WeekStartDay): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val start = date.with(TemporalAdjusters.previousOrSame(weekStartDay.dayOfWeek))
        val end = start.plusDays(6)
        return dateRange(start, end, zoneId)
    }

    fun monthRange(nowMillis: Long, zoneId: ZoneId): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val start = date.withDayOfMonth(1)
        val end = date.withDayOfMonth(date.lengthOfMonth())
        return dateRange(start, end, zoneId)
    }

    private fun dateRange(date: LocalDate, zoneId: ZoneId): Pair<Long, Long> =
        dateRange(date, date, zoneId)

    private fun dateRange(startDate: LocalDate, endDate: LocalDate, zoneId: ZoneId): Pair<Long, Long> {
        val start = LocalDateTime.of(startDate, LocalTime.MIN).atZone(zoneId).toInstant().toEpochMilli()
        val end = LocalDateTime.of(endDate, LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
        return start to end
    }
}
