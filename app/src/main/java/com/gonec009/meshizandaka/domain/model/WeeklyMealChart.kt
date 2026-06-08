package com.gonec009.meshizandaka.domain.model

import java.time.LocalDate

data class DailyMealStack(
    val date: LocalDate,
    val breakfastCalories: Int = 0,
    val lunchCalories: Int = 0,
    val dinnerCalories: Int = 0,
    val snackCalories: Int = 0,
) {
    val totalCalories: Int
        get() = breakfastCalories + lunchCalories + dinnerCalories + snackCalories
}

data class WeeklyMealChart(
    val days: List<DailyMealStack> = emptyList(),
) {
    val maxTotalCalories: Int
        get() = days.maxOfOrNull(DailyMealStack::totalCalories) ?: 0
}

data class HomeDashboardData(
    val summary: DashboardSummary = DashboardSummary(),
    val recentRecords: List<MealRecord> = emptyList(),
    val weeklyChart: WeeklyMealChart = WeeklyMealChart(),
)
