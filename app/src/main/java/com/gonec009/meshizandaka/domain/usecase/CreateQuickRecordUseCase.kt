package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import com.gonec009.meshizandaka.util.TimeRangeUtils
import java.time.ZoneId

class CreateQuickRecordUseCase(
    private val templateRepository: MealTemplateRepository,
    private val recordRepository: MealRecordRepository,
) {
    suspend operator fun invoke(
        templateId: Long,
        selectedOptionIds: Collection<Long> = emptyList(),
        templateNameSnapshot: String? = null,
        totalCaloriesOverride: Int? = null,
        proteinOverride: Double? = null,
        fatOverride: Double? = null,
        carbOverride: Double? = null,
        isSpecialOverride: Boolean? = null,
        memo: String = "",
        photoUri: String? = null,
        mealType: MealType? = null,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val template = templateRepository.getTemplate(templateId) ?: error("Template not found: $templateId")
        val recordMealType = mealType ?: template.mealType
        ensureDailyMealSlotAvailable(recordMealType, nowMillis, zoneId)
        val selectedOptions = template.optionGroups.flatMap { group ->
            group.options.filter { it.id in selectedOptionIds }.map { option ->
                MealRecordOption(
                    optionGroupNameSnapshot = group.name,
                    optionNameSnapshot = option.name,
                    calorieDelta = option.calorieDelta,
                    proteinDeltaG = option.proteinDeltaG,
                    fatDeltaG = option.fatDeltaG,
                    carbDeltaG = option.carbDeltaG,
                )
            }
        }

        val comparisonCalories = template.comparisonTemplateId
            ?.let { comparisonTemplateId ->
                templateRepository.getTemplate(comparisonTemplateId)?.let { comparisonTemplate ->
                    comparisonTemplate.baseCalories + comparisonTemplate.optionGroups.sumOf { group ->
                        group.options.firstOrNull()?.calorieDelta ?: 0
                    }
                }
            }
            ?: 0
        val totalCalories = totalCaloriesOverride ?: template.baseCalories + selectedOptions.sumOf { it.calorieDelta }
        val protein = proteinOverride ?: template.proteinG + selectedOptions.sumOf { it.proteinDeltaG }
        val fat = fatOverride ?: template.fatG + selectedOptions.sumOf { it.fatDeltaG }
        val carb = carbOverride ?: template.carbG + selectedOptions.sumOf { it.carbDeltaG }
        val isSpecial = isSpecialOverride ?: template.isSpecial

        return recordRepository.insertRecord(
            MealRecord(
                eatenAt = nowMillis,
                mealType = recordMealType,
                templateId = template.id,
                templateNameSnapshot = templateNameSnapshot ?: template.name,
                totalCalories = totalCalories,
                proteinG = protein,
                fatG = fat,
                carbG = carb,
                isSpecial = isSpecial,
                specialDeltaCalories = if (isSpecial) totalCalories - comparisonCalories else 0,
                sourceType = SourceType.QUICK_BUTTON,
                memo = memo,
                photoUri = photoUri,
                selectedOptions = selectedOptions,
            ),
        )
    }

    private suspend fun ensureDailyMealSlotAvailable(
        mealType: MealType,
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        if (!mealType.requiresSingleRecordPerDay()) return
        val (startInclusive, endInclusive) = TimeRangeUtils.todayRange(nowMillis, zoneId)
        if (recordRepository.existsRecordForMealTypeBetween(mealType, startInclusive, endInclusive)) {
            throw DuplicateDailyMealException(mealType)
        }
    }

    private fun MealType.requiresSingleRecordPerDay(): Boolean {
        return this == MealType.BREAKFAST || this == MealType.LUNCH || this == MealType.DINNER
    }
}
