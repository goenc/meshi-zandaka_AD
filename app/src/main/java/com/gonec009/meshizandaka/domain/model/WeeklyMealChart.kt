package com.gonec009.meshizandaka.domain.model

import java.time.LocalDate

data class DailyMealStack(
    val date: LocalDate,
    val breakfastCalories: Int = 0,
    val breakfastRecords: List<MealRecord> = emptyList(),
    val lunchCalories: Int = 0,
    val lunchRecords: List<MealRecord> = emptyList(),
    val dinnerCalories: Int = 0,
    val dinnerRecords: List<MealRecord> = emptyList(),
    val snackCalories: Int = 0,
    val snackRecords: List<MealRecord> = emptyList(),
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
    val weeklyChart: WeeklyMealChart = WeeklyMealChart(),
)
