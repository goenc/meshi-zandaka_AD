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

    @Test
    fun 既知日の消費カロリーと未知日の平均値で残高を計算する() {
        val records = listOf(
            recordAt(2026, 6, 8, 12, 0, 1400),
            recordAt(2026, 6, 7, 19, 0, 900),
            recordAt(2026, 6, 1, 12, 0, 700),
        )

        val summary = calculator.buildSummary(
            records = records,
            settings = AppSettings(
                targetCaloriesPerDay = 1800,
                maintenanceCaloriesPerDay = 2000,
                weekStartsOn = WeekStartDay.MONDAY,
            ),
            caloriesByDate = mapOf(
                java.time.LocalDate.of(2026, 6, 1) to 1900,
                java.time.LocalDate.of(2026, 6, 8) to 2100,
                java.time.LocalDate.of(2026, 6, 9) to 2200,
            ),
            averageBurnedCalories = 2000,
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        assertEquals(700, summary.todayBalanceCalories)
        assertEquals(12900, summary.weekBalanceCalories)
        assertEquals(57200, summary.monthBalanceCalories)
    }

    @Test
    fun 直近7日グラフで食事区分ごとに集計できる() {
        val records = listOf(
            recordAt(2026, 6, 8, 7, 30, 300, mealType = MealType.BREAKFAST),
            recordAt(2026, 6, 8, 12, 0, 650, mealType = MealType.LUNCH),
            recordAt(2026, 6, 8, 19, 0, 700, mealType = MealType.DINNER),
            recordAt(2026, 6, 8, 21, 0, 200, mealType = MealType.SNACK),
            recordAt(2026, 6, 7, 20, 0, 900, mealType = MealType.EATING_OUT),
        )

        val chart = calculator.buildWeeklyChart(
            records = records,
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        assertEquals(92, chart.days.size)
        assertEquals(300, chart.days.last().breakfastCalories)
        assertEquals(650, chart.days.last().lunchCalories)
        assertEquals(700, chart.days.last().dinnerCalories)
        assertEquals(200, chart.days.last().snackCalories)
        assertEquals(1850, chart.days.last().totalCalories)
        assertEquals(900, chart.days[chart.days.lastIndex - 1].snackCalories)
    }

    @Test
    fun 直近7日グラフは記録がない日も0で埋める() {
        val chart = calculator.buildWeeklyChart(
            records = listOf(recordAt(2026, 6, 8, 12, 0, 650, mealType = MealType.LUNCH)),
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        assertEquals(92, chart.days.size)
        assertEquals(0, chart.days.first().totalCalories)
        assertEquals(650, chart.days.last().totalCalories)
    }

    @Test
    fun グラフ詳細表示用に食事区分ごとの記録一覧を保持する() {
        val chart = calculator.buildWeeklyChart(
            records = listOf(
                recordAt(2026, 6, 8, 7, 30, 300, mealType = MealType.BREAKFAST),
                recordAt(2026, 6, 8, 12, 0, 650, mealType = MealType.LUNCH),
                recordAt(2026, 6, 8, 19, 0, 700, mealType = MealType.DINNER),
                recordAt(2026, 6, 8, 21, 0, 200, mealType = MealType.SNACK),
                recordAt(2026, 6, 7, 20, 0, 900, mealType = MealType.EATING_OUT),
            ),
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        assertEquals(1, chart.days.last().breakfastRecords.size)
        assertEquals(1, chart.days.last().lunchRecords.size)
        assertEquals(1, chart.days.last().dinnerRecords.size)
        assertEquals(1, chart.days.last().snackRecords.size)
        assertEquals(MealType.SNACK, chart.days.last().snackRecords.single().mealType)
        assertEquals(MealType.EATING_OUT, chart.days[chart.days.lastIndex - 1].snackRecords.single().mealType)
    }

    @Test
    fun 間食を間朝間昼間全の三枠で集計する() {
        val chart = calculator.buildWeeklyChart(
            records = listOf(
                recordAt(2026, 6, 8, 10, 0, 100, mealType = MealType.MORNING_SNACK),
                recordAt(2026, 6, 8, 15, 0, 200, mealType = MealType.DAYTIME_SNACK),
                recordAt(2026, 6, 8, 22, 0, 300, mealType = MealType.FREE_SNACK),
            ),
            zoneId = zoneId,
            nowMillis = nowMillis,
        )

        val today = chart.days.last()
        assertEquals(100, today.morningSnackCalories)
        assertEquals(200, today.daytimeSnackCalories)
        assertEquals(300, today.freeSnackCalories)
        assertEquals(600, today.snackCalories)
        assertEquals(600, today.totalCalories)
    }

    private fun recordAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        calories: Int,
        isSpecial: Boolean = false,
        specialDelta: Int = 0,
        mealType: MealType = MealType.DINNER,
    ): MealRecord {
        return MealRecord(
            eatenAt = LocalDateTime.of(year, month, day, hour, minute)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
            mealType = mealType,
            templateId = null,
            templateNameSnapshot = "テスト",
            totalCalories = calories,
            proteinG = 0.0,
            fatG = 0.0,
            carbG = 0.0,
            isSpecial = isSpecial,
            specialDeltaCalories = specialDelta,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
        )
    }
}
