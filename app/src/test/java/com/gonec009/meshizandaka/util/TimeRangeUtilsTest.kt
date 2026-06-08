package com.gonec009.meshizandaka.util

import com.gonec009.meshizandaka.domain.model.WeekStartDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimeRangeUtilsTest {
    private val zoneId = ZoneId.of("Asia/Tokyo")

    @Test
    fun 月初をまたぐ週でも開始日を正しく返す() {
        val now = Instant.parse("2026-06-01T03:00:00Z").toEpochMilli()
        val range = TimeRangeUtils.weekRange(now, zoneId, WeekStartDay.MONDAY)
        val start = Instant.ofEpochMilli(range.first).atZone(zoneId).toLocalDate()
        val end = Instant.ofEpochMilli(range.second).atZone(zoneId).toLocalDate()

        assertEquals("2026-06-01", start.toString())
        assertEquals("2026-06-07", end.toString())
    }

    @Test
    fun 日曜開始なら前月末から週が始まる() {
        val now = Instant.parse("2026-06-01T03:00:00Z").toEpochMilli()
        val range = TimeRangeUtils.weekRange(now, zoneId, WeekStartDay.SUNDAY)
        val start = Instant.ofEpochMilli(range.first).atZone(zoneId).toLocalDate()

        assertEquals("2026-05-31", start.toString())
    }
}
