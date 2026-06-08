package com.gonec009.meshizandaka.domain.model

import com.gonec009.meshizandaka.domain.model.WeekStartDay

data class AppSettings(
    val targetCaloriesPerDay: Int = 1800,
    val maintenanceCaloriesPerDay: Int = 2000,
    val weekStartsOn: WeekStartDay = WeekStartDay.MONDAY,
    val defaultBreakfastTemplateId: Long? = null,
    val defaultLunchTemplateId: Long? = null,
    val defaultDinnerTemplateId: Long? = null,
)
