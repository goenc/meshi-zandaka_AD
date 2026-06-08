package com.gonec009.meshizandaka.data

import android.content.Context
import com.gonec009.meshizandaka.data.local.AppDatabase
import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.data.repository.SettingsRepository
import com.gonec009.meshizandaka.domain.service.BudgetCalculator
import com.gonec009.meshizandaka.domain.usecase.CreateQuickRecordUseCase
import com.gonec009.meshizandaka.domain.usecase.EnsureSeedDataUseCase
import com.gonec009.meshizandaka.domain.usecase.ObserveDashboardUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.create(appContext) }

    val settingsRepository by lazy { SettingsRepository(appContext) }
    val mealTemplateRepository by lazy { MealTemplateRepository(database.mealTemplateDao()) }
    val mealRecordRepository by lazy { MealRecordRepository(database.mealRecordDao()) }
    val budgetCalculator by lazy { BudgetCalculator() }

    val ensureSeedDataUseCase by lazy {
        EnsureSeedDataUseCase(
            templateRepository = mealTemplateRepository,
            settingsRepository = settingsRepository,
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
        )
    }
}
