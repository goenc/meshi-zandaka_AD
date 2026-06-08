package com.gonec009.meshizandaka.domain.service

import com.gonec009.meshizandaka.domain.model.AppSettings
import com.gonec009.meshizandaka.domain.model.DailyMealStack
import com.gonec009.meshizandaka.domain.model.DashboardSummary
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.WeeklyMealChart
import com.gonec009.meshizandaka.util.TimeRangeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BudgetCalculator {
    fun buildSummary(
        records: List<MealRecord>,
        settings: AppSettings,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): DashboardSummary {
        val todayRange = TimeRangeUtils.todayRange(nowMillis, zoneId)
        val weekRange = TimeRangeUtils.weekRange(nowMillis, zoneId, settings.weekStartsOn)
        val monthRange = TimeRangeUtils.monthRange(nowMillis, zoneId)

        val todayRecords = records.filter { it.eatenAt in todayRange.first..todayRange.second }
        val weekRecords = records.filter { it.eatenAt in weekRange.first..weekRange.second }
        val monthRecords = records.filter { it.eatenAt in monthRange.first..monthRange.second }

        val todayConsumed = todayRecords.sumOf(MealRecord::totalCalories)
        val weekConsumed = weekRecords.sumOf(MealRecord::totalCalories)
        val monthConsumed = monthRecords.sumOf(MealRecord::totalCalories)

        return DashboardSummary(
            todayConsumedCalories = todayConsumed,
            todayBalanceCalories = settings.targetCaloriesPerDay - todayConsumed,
            weekBalanceCalories = (settings.targetCaloriesPerDay * 7) - weekConsumed,
            monthBalanceCalories = (settings.targetCaloriesPerDay * TimeRangeUtils.daysInCurrentMonth(nowMillis, zoneId)) - monthConsumed,
            monthSpecialCount = monthRecords.count { it.isSpecial },
            monthSpecialDeltaCalories = monthRecords.sumOf(MealRecord::specialDeltaCalories),
        )
    }

    fun buildWeeklyChart(
        records: List<MealRecord>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): WeeklyMealChart {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startDate = today.minusDays(6)
        val recordsByDate = records.groupBy { Instant.ofEpochMilli(it.eatenAt).atZone(zoneId).toLocalDate() }

        return WeeklyMealChart(
            days = (0L..6L).map { offset ->
                val date = startDate.plusDays(offset)
                buildDailyMealStack(date, recordsByDate[date].orEmpty())
            },
        )
    }

    private fun buildDailyMealStack(
        date: LocalDate,
        records: List<MealRecord>,
    ): DailyMealStack {
        return DailyMealStack(
            date = date,
            breakfastCalories = records.filter { it.mealType == MealType.BREAKFAST }.sumOf(MealRecord::totalCalories),
            lunchCalories = records.filter { it.mealType == MealType.LUNCH }.sumOf(MealRecord::totalCalories),
            dinnerCalories = records.filter { it.mealType == MealType.DINNER }.sumOf(MealRecord::totalCalories),
            snackCalories = records.filter { it.mealType == MealType.SNACK || it.mealType == MealType.EATING_OUT }.sumOf(MealRecord::totalCalories),
        )
    }
}
