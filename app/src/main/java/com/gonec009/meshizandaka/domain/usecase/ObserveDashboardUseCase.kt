package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.SettingsRepository
import com.gonec009.meshizandaka.domain.model.HomeDashboardData
import com.gonec009.meshizandaka.domain.service.BudgetCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId

class ObserveDashboardUseCase(
    private val settingsRepository: SettingsRepository,
    private val recordRepository: MealRecordRepository,
    private val budgetCalculator: BudgetCalculator,
) {
    operator fun invoke(zoneId: ZoneId = ZoneId.systemDefault()): Flow<HomeDashboardData> {
        val now = System.currentTimeMillis()
        val observeStart = Instant.ofEpochMilli(now).atZone(zoneId).minusDays(40).toInstant().toEpochMilli()
        val observeEnd = Instant.ofEpochMilli(now).atZone(zoneId).plusDays(2).toInstant().toEpochMilli()
        return combine(
            settingsRepository.settingsFlow,
            recordRepository.observeRecordsBetween(observeStart, observeEnd),
            recordRepository.observeRecentRecords(),
        ) { settings, monthRecords, recentRecords ->
            HomeDashboardData(
                summary = budgetCalculator.buildSummary(monthRecords, settings, zoneId),
                recentRecords = recentRecords,
                weeklyChart = budgetCalculator.buildWeeklyChart(monthRecords, zoneId),
            )
        }
    }
}
