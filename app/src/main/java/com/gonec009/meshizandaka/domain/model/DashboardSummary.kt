package com.gonec009.meshizandaka.domain.model

data class DashboardSummary(
    val todayConsumedCalories: Int = 0,
    val todayProteinGrams: Double = 0.0,
    val todayFatGrams: Double = 0.0,
    val todayCarbGrams: Double = 0.0,
    val todayBalanceCalories: Int = 0,
    val weekBalanceCalories: Int = 0,
    val monthBalanceCalories: Int = 0,
    val monthSpecialCount: Int = 0,
    val monthSpecialDeltaCalories: Int = 0,
)
