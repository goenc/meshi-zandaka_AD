package com.gonec009.meshizandaka.domain.model

data class AppSettings(
    val targetCaloriesPerDay: Int = 1800,
    val maintenanceCaloriesPerDay: Int = 2000,
    val weekStartsOn: WeekStartDay = WeekStartDay.MONDAY,
)
