package com.gonec009.meshizandaka.domain.model

import java.time.LocalDate

data class DailyMealStack(
    val date: LocalDate,
    val breakfastCalories: Int = 0,
    val breakfastRecords: List<MealRecord> = emptyList(),
    val morningSnackCalories: Int = 0,
    val morningSnackRecords: List<MealRecord> = emptyList(),
    val lunchCalories: Int = 0,
    val lunchRecords: List<MealRecord> = emptyList(),
    val dinnerCalories: Int = 0,
    val dinnerRecords: List<MealRecord> = emptyList(),
    val daytimeSnackCalories: Int = 0,
    val daytimeSnackRecords: List<MealRecord> = emptyList(),
    val freeSnackCalories: Int = 0,
    val freeSnackRecords: List<MealRecord> = emptyList(),
) {
    val snackCalories: Int
        get() = morningSnackCalories + daytimeSnackCalories + freeSnackCalories

    val snackRecords: List<MealRecord>
        get() = morningSnackRecords + daytimeSnackRecords + freeSnackRecords

    val totalCalories: Int
        get() = breakfastCalories +
            morningSnackCalories +
            lunchCalories +
            dinnerCalories +
            daytimeSnackCalories +
            freeSnackCalories
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
