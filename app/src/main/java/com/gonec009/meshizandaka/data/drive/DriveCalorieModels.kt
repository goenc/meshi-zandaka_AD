package com.gonec009.meshizandaka.data.drive

import java.time.LocalDate
import kotlin.math.roundToInt

data class DriveCalorieSummary(
    val todayKcal: Int? = null,
    val averageKcal: Int? = null,
    val calorieDate: LocalDate? = null,
)

internal fun buildDriveCalorieSummary(
    rows: List<List<Any?>>,
    today: LocalDate,
): DriveCalorieSummary {
    val header = rows.firstOrNull().orEmpty()
    val targetDateIndex = header.indexOfFirst { value ->
        value?.toString()?.equals("targetDate", ignoreCase = true) == true ||
            value?.toString()?.equals("target_date", ignoreCase = true) == true
    }
    val calorieIndex = header.indexOfFirst { value ->
        value?.toString()?.equals("estimatedTotalKcal", ignoreCase = true) == true
    }
    if (targetDateIndex < 0 || calorieIndex < 0) return DriveCalorieSummary()

    val dailyCalories = linkedMapOf<LocalDate, Int>()
    rows.drop(1).forEach { row ->
        val targetDate = parseSheetDate(row.getOrNull(targetDateIndex)) ?: return@forEach
        val calories = parseCalories(row.getOrNull(calorieIndex)) ?: return@forEach
        if (calories >= 0) dailyCalories[targetDate] = calories
    }

    val latestEntry = dailyCalories.maxByOrNull { it.key }
    val selectedEntry = dailyCalories[today]?.let { today to it }
        ?: latestEntry?.let { it.key to it.value }
    val values = dailyCalories.values.toList()
    return DriveCalorieSummary(
        todayKcal = selectedEntry?.second,
        averageKcal = values.takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt(),
        calorieDate = selectedEntry?.first,
    )
}

private fun parseSheetDate(value: Any?): LocalDate? {
    return when (value) {
        is Number -> runCatching {
            LocalDate.of(1899, 12, 30).plusDays(value.toLong())
        }.getOrNull()
        is String -> runCatching {
            LocalDate.parse(value.trim().take(10))
        }.getOrNull()
        else -> null
    }
}

private fun parseCalories(value: Any?): Int? {
    return when (value) {
        is Number -> value.toDouble().takeIf { it.isFinite() }?.roundToInt()
        is String -> value.replace(",", "").trim().toDoubleOrNull()?.roundToInt()
        else -> null
    }
}
