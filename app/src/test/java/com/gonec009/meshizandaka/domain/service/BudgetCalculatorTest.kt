package com.gonec009.meshizandaka.domain.service

import com.gonec009.meshizandaka.domain.model.AppSettings
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import com.gonec009.meshizandaka.domain.model.WeekStartDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BudgetCalculatorTest {
    private val zoneId = ZoneId.of("Asia/Tokyo")
    private val calculator = BudgetCalculator()
    private val nowMillis = Instant.parse("2026-06-08T12:00:00Z").toEpochMilli()

    @Test
    fun 今日今週今月の残高を正しく集計する() {
        val records = listOf(
            recordAt(2026, 6, 8, 12, 0, 650, false, 0),
            recordAt(2026, 6, 8, 19, 0, 750, true, 300),
            recordAt(2026, 6, 7, 19, 0, 900, false, 0),
            recordAt(2026, 6, 1, 12, 0, 700, false, 0),
        )

        val summary = calculator.buildSummary(
            records = records,
            settings = AppSettings(
                targetCaloriesPerDay = 1800,
                maintenanceCaloriesPerDay = 2000,
                weekStartsOn = WeekStartDay.MONDAY,
            ),
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        assertEquals(1400, summary.todayConsumedCalories)
        assertEquals(400, summary.todayBalanceCalories)
        assertEquals(11200, summary.weekBalanceCalories)
        assertEquals(51000, summary.monthBalanceCalories)
        assertEquals(1, summary.monthSpecialCount)
        assertEquals(300, summary.monthSpecialDeltaCalories)
    }

    @Test
    fun 週開始曜日が日曜なら前日を同一週として扱う() {
        val records = listOf(
            recordAt(2026, 6, 7, 10, 0, 1000, false, 0),
            recordAt(2026, 6, 8, 10, 0, 800, false, 0),
        )

        val summary = calculator.buildSummary(
            records = records,
            settings = AppSettings(
                targetCaloriesPerDay = 1800,
                maintenanceCaloriesPerDay = 2000,
                weekStartsOn = WeekStartDay.SUNDAY,
            ),
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        assertEquals(10800, summary.weekBalanceCalories)
    }

    private fun recordAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        calories: Int,
        isSpecial: Boolean,
        specialDelta: Int,
    ): MealRecord {
        return MealRecord(
            eatenAt = LocalDateTime.of(year, month, day, hour, minute)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
            mealType = MealType.DINNER,
            templateId = null,
            templateNameSnapshot = "テスト",
            totalCalories = calories,
            proteinG = 0,
            fatG = 0,
            carbG = 0,
            isSpecial = isSpecial,
            specialDeltaCalories = specialDelta,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
        )
    }
}
