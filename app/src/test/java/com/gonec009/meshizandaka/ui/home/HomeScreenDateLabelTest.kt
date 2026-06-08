package com.gonec009.meshizandaka.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeScreenDateLabelTest {
    @Test
    fun 月初の翌日も月日表記にする() {
        val label = formatChartDateLabel(
            index = 2,
            date = LocalDate.of(2026, 6, 2),
            previousDate = LocalDate.of(2026, 6, 1),
        )

        assertEquals("6/2", label)
    }

    @Test
    fun 通常日は日だけを表示する() {
        val label = formatChartDateLabel(
            index = 3,
            date = LocalDate.of(2026, 6, 3),
            previousDate = LocalDate.of(2026, 6, 2),
        )

        assertEquals("3", label)
    }
}
