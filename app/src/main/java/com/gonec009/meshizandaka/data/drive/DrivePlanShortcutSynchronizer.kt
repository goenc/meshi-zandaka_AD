package com.gonec009.meshizandaka.data.drive

import com.gonec009.meshizandaka.data.repository.MealTemplateRepository

class DrivePlanShortcutSynchronizer(
    private val mealTemplateRepository: MealTemplateRepository,
) {
    suspend fun sync(plan: DrivePlan) {
        mealTemplateRepository.syncDriveShortcuts(plan)
    }
}
