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
        caloriesByDate: Map<LocalDate, Int> = emptyMap(),
        averageBurnedCalories: Int? = null,
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
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(settings.weekStartsOn.dayOfWeek))
        val weekEnd = weekStart.plusDays(6)
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val fallbackBurnedCalories = averageBurnedCalories ?: settings.targetCaloriesPerDay

        return DashboardSummary(
            todayConsumedCalories = todayConsumed,
            todayBalanceCalories = burnedCaloriesBetween(
                startDate = today,
                endDate = today,
                caloriesByDate = caloriesByDate,
                fallbackCalories = fallbackBurnedCalories,
            ) - todayConsumed,
            weekBalanceCalories = burnedCaloriesBetween(
                startDate = weekStart,
                endDate = weekEnd,
                caloriesByDate = caloriesByDate,
                fallbackCalories = fallbackBurnedCalories,
            ) - weekConsumed,
            monthBalanceCalories = burnedCaloriesBetween(
                startDate = monthStart,
                endDate = monthEnd,
                caloriesByDate = caloriesByDate,
                fallbackCalories = fallbackBurnedCalories,
            ) - monthConsumed,
            monthSpecialCount = monthRecords.count { it.isSpecial },
            monthSpecialDeltaCalories = monthRecords.sumOf(MealRecord::specialDeltaCalories),
        )
    }

    private fun burnedCaloriesBetween(
        startDate: LocalDate,
        endDate: LocalDate,
        caloriesByDate: Map<LocalDate, Int>,
        fallbackCalories: Int,
    ): Int {
        var date = startDate
        var total = 0
        while (!date.isAfter(endDate)) {
            total += caloriesByDate[date] ?: fallbackCalories
            date = date.plusDays(1)
        }
        return total
    }

    fun buildWeeklyChart(
        records: List<MealRecord>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): WeeklyMealChart {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startDate = today.minusMonths(3).plusDays(1)
        val recordsByDate = records.groupBy { Instant.ofEpochMilli(it.eatenAt).atZone(zoneId).toLocalDate() }
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)

        return WeeklyMealChart(
            days = (0L..totalDays).map { offset ->
                val date = startDate.plusDays(offset)
                buildDailyMealStack(date, recordsByDate[date].orEmpty())
            },
        )
    }

    private fun buildDailyMealStack(
        date: LocalDate,
        records: List<MealRecord>,
    ): DailyMealStack {
        val breakfastRecords = records.filter { it.mealType == MealType.BREAKFAST }
        val morningSnackRecords = records.filter { it.mealType == MealType.MORNING_SNACK }
        val lunchRecords = records.filter { it.mealType == MealType.LUNCH }
        val dinnerRecords = records.filter { it.mealType == MealType.DINNER }
        val daytimeSnackRecords = records.filter { it.mealType == MealType.DAYTIME_SNACK }
        val freeSnackRecords = records.filter {
            it.mealType == MealType.FREE_SNACK ||
                it.mealType == MealType.SNACK ||
                it.mealType == MealType.EATING_OUT
        }
        return DailyMealStack(
            date = date,
            breakfastRecords = breakfastRecords,
            breakfastCalories = breakfastRecords.sumOf(MealRecord::totalCalories),
            morningSnackRecords = morningSnackRecords,
            morningSnackCalories = morningSnackRecords.sumOf(MealRecord::totalCalories),
            lunchRecords = lunchRecords,
            lunchCalories = lunchRecords.sumOf(MealRecord::totalCalories),
            dinnerRecords = dinnerRecords,
            dinnerCalories = dinnerRecords.sumOf(MealRecord::totalCalories),
            daytimeSnackRecords = daytimeSnackRecords,
            daytimeSnackCalories = daytimeSnackRecords.sumOf(MealRecord::totalCalories),
            freeSnackRecords = freeSnackRecords,
            freeSnackCalories = freeSnackRecords.sumOf(MealRecord::totalCalories),
        )
    }
}
