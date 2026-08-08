package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.SettingsRepository
import com.gonec009.meshizandaka.domain.model.HomeDashboardData
import com.gonec009.meshizandaka.domain.service.BudgetCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ObserveDashboardUseCase(
    private val settingsRepository: SettingsRepository,
    private val recordRepository: MealRecordRepository,
    private val budgetCalculator: BudgetCalculator,
    private val caloriesByDateFlow: Flow<Map<LocalDate, Int>> = flowOf(emptyMap()),
    private val averageBurnedCaloriesFlow: Flow<Int?> = flowOf(null),
) {
    operator fun invoke(zoneId: ZoneId = ZoneId.systemDefault()): Flow<HomeDashboardData> {
        val now = System.currentTimeMillis()
        val observeStart = Instant.ofEpochMilli(now).atZone(zoneId).minusMonths(3).plusDays(1).toInstant().toEpochMilli()
        val observeEnd = Instant.ofEpochMilli(now).atZone(zoneId).plusDays(2).toInstant().toEpochMilli()
        return combine(
            settingsRepository.settingsFlow,
            recordRepository.observeRecordsBetween(observeStart, observeEnd),
            caloriesByDateFlow,
            averageBurnedCaloriesFlow,
        ) { settings, monthRecords, caloriesByDate, averageBurnedCalories ->
            HomeDashboardData(
                summary = budgetCalculator.buildSummary(
                    records = monthRecords,
                    settings = settings,
                    caloriesByDate = caloriesByDate,
                    averageBurnedCalories = averageBurnedCalories,
                    zoneId = zoneId,
                ),
                weeklyChart = budgetCalculator.buildWeeklyChart(monthRecords, zoneId),
            )
        }
    }
}
