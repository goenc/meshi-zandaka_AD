package com.gonec009.meshizandaka.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeScreenDateLabelTest {
    @Test
    fun 左端に見えている棒は月日表記にする() {
        val label = formatChartDateLabel(
            date = LocalDate.of(2026, 5, 22),
            isLeftVisible = true,
        )

        assertEquals("5/22", label)
    }

    @Test
    fun 左端以外の棒は日だけを表示する() {
        val label = formatChartDateLabel(
            date = LocalDate.of(2026, 5, 23),
            isLeftVisible = false,
        )

        assertEquals("23", label)
    }

    @Test
    fun スクロール位置から左端の棒インデックスを求める() {
        val index = calculateLeftVisibleChartIndex(
            scrollOffsetPx = 95,
            barWidthPx = 39f,
            barSpacingPx = 8f,
            itemCount = 10,
        )

        assertEquals(2, index)
    }
}
