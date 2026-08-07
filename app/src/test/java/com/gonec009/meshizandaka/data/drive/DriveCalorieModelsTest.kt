package com.gonec009.meshizandaka.data.drive

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveCalorieModelsTest {
    @Test
    fun 今日の値と登録済み日次データの平均を計算する() {
        val summary = buildDriveCalorieSummary(
            rows = listOf(
                listOf("targetDate", "steps", "estimatedTotalKcal"),
                listOf("2026-08-08", 5_000, 2_000),
                listOf("2026-08-07", 20_000, 2_201),
                listOf("2026-08-06", 18_000, 2_400),
            ),
            today = LocalDate.of(2026, 8, 8),
        )

        assertEquals(2_000, summary.todayKcal)
        assertEquals(2_200, summary.averageKcal)
        assertEquals(LocalDate.of(2026, 8, 8), summary.calorieDate)
    }

    @Test
    fun 今日の値が空なら最新の推定値を使う() {
        val summary = buildDriveCalorieSummary(
            rows = listOf(
                listOf("targetDate", "estimatedTotalKcal"),
                listOf("2026-08-08"),
                listOf("2026-08-07", "2,200"),
            ),
            today = LocalDate.of(2026, 8, 8),
        )

        assertEquals(2_200, summary.todayKcal)
        assertEquals(2_200, summary.averageKcal)
        assertEquals(LocalDate.of(2026, 8, 7), summary.calorieDate)
    }
}
