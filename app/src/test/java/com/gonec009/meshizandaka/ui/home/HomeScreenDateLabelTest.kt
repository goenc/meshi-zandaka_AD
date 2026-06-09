package com.gonec009.meshizandaka.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeScreenDateLabelTest {
    @Test
    fun カロリーは固定量ごとのブロック数に変換する() {
        assertEquals(1, chartBlockCount(1))
        assertEquals(1, chartBlockCount(100))
        assertEquals(2, chartBlockCount(101))
        assertEquals(7, chartBlockCount(650))
    }

    @Test
    fun 月初の一日だけ月日表記にする() {
        val label = formatChartDateLabel(
            date = LocalDate.of(2026, 6, 1),
            previousDate = LocalDate.of(2026, 5, 31),
        )

        assertEquals("6/1", label)
    }

    @Test
    fun 月初以外は日だけを表示する() {
        val label = formatChartDateLabel(
            date = LocalDate.of(2026, 6, 2),
            previousDate = LocalDate.of(2026, 6, 1),
        )

        assertEquals("2", label)
    }
}
