package com.gonec009.meshizandaka.data.drive

import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.data.repository.SettingsRepository
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import kotlinx.coroutines.flow.first

class DrivePlanShortcutSynchronizer(
    private val mealTemplateRepository: MealTemplateRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun sync(plan: DrivePlan) {
        val syncedIds = mealTemplateRepository.syncDriveShortcuts(plan)
        if (syncedIds.isEmpty()) return

        val current = settingsRepository.settingsFlow.first()
        settingsRepository.updateSettings(
            current.copy(
                defaultBreakfastTemplateId = syncedIds[TemplateShortcutRole.BREAKFAST]
                    ?: current.defaultBreakfastTemplateId,
                defaultLunchTemplateId = syncedIds[TemplateShortcutRole.LUNCH]
                    ?: current.defaultLunchTemplateId,
                defaultDinnerTemplateId = syncedIds[TemplateShortcutRole.DINNER]
                    ?: current.defaultDinnerTemplateId,
            ),
        )
    }
}
