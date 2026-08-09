package com.gonec009.meshizandaka.data

import android.content.Context
import com.gonec009.meshizandaka.data.drive.DriveAccessManager
import com.gonec009.meshizandaka.data.drive.GoogleDriveClient
import com.gonec009.meshizandaka.data.drive.DrivePlanShortcutSynchronizer
import com.gonec009.meshizandaka.data.local.AppDatabase
import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.data.repository.DrivePlanCacheRepository
import com.gonec009.meshizandaka.data.repository.SettingsRepository
import com.gonec009.meshizandaka.domain.service.BudgetCalculator
import com.gonec009.meshizandaka.domain.usecase.CreateQuickRecordUseCase
import com.gonec009.meshizandaka.domain.usecase.EnsureSeedDataUseCase
import com.gonec009.meshizandaka.domain.usecase.ObserveDashboardUseCase
import kotlinx.coroutines.flow.map

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.create(appContext) }

    val settingsRepository by lazy { SettingsRepository(appContext) }
    val mealTemplateRepository by lazy { MealTemplateRepository(database.mealTemplateDao(), appContext) }
    val mealRecordRepository by lazy { MealRecordRepository(database.mealRecordDao(), appContext) }
    val drivePlanCacheRepository by lazy { DrivePlanCacheRepository(database.drivePlanCacheDao()) }
    val drivePlanShortcutSynchronizer by lazy {
        DrivePlanShortcutSynchronizer(
            mealTemplateRepository = mealTemplateRepository,
        )
    }
    val budgetCalculator by lazy { BudgetCalculator() }
    val driveAccessManager by lazy {
        DriveAccessManager(
            client = GoogleDriveClient(),
            context = appContext,
            cacheRepository = drivePlanCacheRepository,
            shortcutSynchronizer = drivePlanShortcutSynchronizer,
        )
    }

    val ensureSeedDataUseCase by lazy {
        EnsureSeedDataUseCase(
            templateRepository = mealTemplateRepository,
        )
    }
    val createQuickRecordUseCase by lazy {
        CreateQuickRecordUseCase(
            templateRepository = mealTemplateRepository,
            recordRepository = mealRecordRepository,
        )
    }
    val observeDashboardUseCase by lazy {
        ObserveDashboardUseCase(
            settingsRepository = settingsRepository,
            recordRepository = mealRecordRepository,
            budgetCalculator = budgetCalculator,
            caloriesByDateFlow = driveAccessManager.calorieSummary.map { it?.caloriesByDate.orEmpty() },
            averageBurnedCaloriesFlow = driveAccessManager.calorieSummary.map { it?.averageKcal },
        )
    }
}
