package com.gonec009.meshizandaka.data.drive

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DriveCalorieSummaryCacheTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearCacheBeforeTest() {
        clearCache()
    }

    @After
    fun clearCacheAfterTest() {
        clearCache()
    }

    @Test
    fun 最新平均日別カロリーを保存して復元できる() {
        val expected = DriveCalorieSummary(
            todayKcal = 2_172,
            averageKcal = 2_159,
            calorieDate = LocalDate.of(2026, 8, 9),
            caloriesByDate = mapOf(
                LocalDate.of(2026, 8, 8) to 2_140,
                LocalDate.of(2026, 8, 9) to 2_172,
            ),
        )

        DriveCalorieSummaryCache(context).save(expected)

        assertEquals(expected, DriveCalorieSummaryCache(context).load())
    }

    private fun clearCache() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "drive_calorie_summary"
    }
}
