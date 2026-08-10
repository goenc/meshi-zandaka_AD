package com.gonec009.meshizandaka.data.drive

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

internal class DriveCalorieSummaryCache(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): DriveCalorieSummary? {
        val caloriesByDate = preferences.getStringSet(KEY_CALORIES_BY_DATE, emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val separatorIndex = entry.indexOf(ENTRY_SEPARATOR)
                if (separatorIndex <= 0 || separatorIndex >= entry.lastIndex) return@mapNotNull null
                val date = runCatching {
                    LocalDate.parse(entry.substring(0, separatorIndex))
                }.getOrNull() ?: return@mapNotNull null
                val calories = entry.substring(separatorIndex + 1).toIntOrNull() ?: return@mapNotNull null
                date to calories
            }
            .toMap()
        val summary = DriveCalorieSummary(
            todayKcal = preferences.intOrNull(KEY_TODAY_KCAL),
            averageKcal = preferences.intOrNull(KEY_AVERAGE_KCAL),
            calorieDate = preferences.getString(KEY_CALORIE_DATE, null)?.let { storedDate ->
                runCatching { LocalDate.parse(storedDate) }.getOrNull()
            },
            caloriesByDate = caloriesByDate,
        )
        return summary.takeIf { it.hasData }
    }

    fun save(summary: DriveCalorieSummary) {
        if (!summary.hasData) return
        preferences.edit {
            putNullableInt(KEY_TODAY_KCAL, summary.todayKcal)
            putNullableInt(KEY_AVERAGE_KCAL, summary.averageKcal)
            if (summary.calorieDate == null) {
                remove(KEY_CALORIE_DATE)
            } else {
                putString(KEY_CALORIE_DATE, summary.calorieDate.toString())
            }
            putStringSet(
                KEY_CALORIES_BY_DATE,
                summary.caloriesByDate.mapTo(linkedSetOf()) { (date, calories) ->
                    "$date$ENTRY_SEPARATOR$calories"
                },
            )
        }
    }

    private fun android.content.SharedPreferences.intOrNull(key: String): Int? {
        return if (contains(key)) getInt(key, 0) else null
    }

    private fun android.content.SharedPreferences.Editor.putNullableInt(key: String, value: Int?) {
        if (value == null) remove(key) else putInt(key, value)
    }

    private companion object {
        const val PREFERENCES_NAME = "drive_calorie_summary"
        const val KEY_TODAY_KCAL = "today_kcal"
        const val KEY_AVERAGE_KCAL = "average_kcal"
        const val KEY_CALORIE_DATE = "calorie_date"
        const val KEY_CALORIES_BY_DATE = "calories_by_date"
        const val ENTRY_SEPARATOR = '='
    }
}
