package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.repository.MealRecordRepository
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import com.gonec009.meshizandaka.domain.model.TemplateShortcutRole
import com.gonec009.meshizandaka.util.TimeRangeUtils
import java.time.ZoneId

class CreateQuickRecordUseCase(
    private val templateRepository: MealTemplateRepository,
    private val recordRepository: MealRecordRepository,
) {
    suspend operator fun invoke(
        templateId: Long?,
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
        appendToRecordId: Long? = null,
        isSetRegistration: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val template = templateId?.let { id ->
            templateRepository.getTemplate(id) ?: error("Template not found: $id")
        }
        val recordMealType = mealType ?: template?.mealType
            ?: error("Meal type is required when templateId is null")
        val shouldPreventDuplicate = isSetRegistration ||
            (template?.shortcutRole != null && template.shortcutRole != TemplateShortcutRole.NONE)
        if (shouldPreventDuplicate) {
            ensureDailySetAvailable(recordMealType, nowMillis, zoneId)
        }
        val appendTarget = appendToRecordId?.let { recordId ->
            recordRepository.getRecord(recordId)?.also { target ->
                validateAppendTarget(target, recordMealType, nowMillis, zoneId)
            } ?: throw RecordAppendTargetException()
        } ?: findAutomaticAppendTarget(recordMealType, nowMillis, zoneId)
        val selectedOptions = template?.optionGroups.orEmpty().flatMap { group ->
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

        val comparisonCalories = template?.comparisonTemplateId
            ?.let { comparisonTemplateId ->
                templateRepository.getTemplate(comparisonTemplateId)?.let { comparisonTemplate ->
                    comparisonTemplate.baseCalories + comparisonTemplate.optionGroups.sumOf { group ->
                        group.options.firstOrNull()?.calorieDelta ?: 0
                    }
                }
            }
            ?: 0
        val totalCalories = totalCaloriesOverride
            ?: (template?.baseCalories ?: 0) + selectedOptions.sumOf { it.calorieDelta }
        val protein = proteinOverride
            ?: (template?.proteinG ?: 0.0) + selectedOptions.sumOf { it.proteinDeltaG }
        val fat = fatOverride
            ?: (template?.fatG ?: 0.0) + selectedOptions.sumOf { it.fatDeltaG }
        val carb = carbOverride
            ?: (template?.carbG ?: 0.0) + selectedOptions.sumOf { it.carbDeltaG }
        val isSpecial = isSpecialOverride ?: template?.isSpecial ?: false

        val record = MealRecord(
            eatenAt = nowMillis,
            mealType = recordMealType,
            templateId = template?.id,
            templateNameSnapshot = templateNameSnapshot ?: template?.name
                ?: error("Template name is required when templateId is null"),
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
        )
        return if (appendTarget != null) {
            if (!recordRepository.appendToRecord(appendTarget.id, record)) {
                throw RecordAppendTargetException()
            }
            appendTarget.id
        } else {
            recordRepository.insertRecord(record)
        }
    }

    private suspend fun ensureDailySetAvailable(
        mealType: MealType,
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        val (startInclusive, endInclusive) = TimeRangeUtils.todayRange(nowMillis, zoneId)
        val alreadyRegistered = recordRepository.getRecordsBetween(startInclusive, endInclusive)
            .any { record -> record.mealType.isSameQuickRecordMealType(mealType) }
        if (alreadyRegistered) {
            throw DuplicateDailyMealException(mealType)
        }
    }

    private suspend fun findAutomaticAppendTarget(
        mealType: MealType,
        nowMillis: Long,
        zoneId: ZoneId,
    ): MealRecord? {
        val (startInclusive, endInclusive) = TimeRangeUtils.todayRange(nowMillis, zoneId)
        val records = recordRepository.getRecordsBetween(startInclusive, endInclusive)
        records.firstOrNull { record -> record.mealType.isSameQuickRecordMealType(mealType) }?.let { record ->
            return record
        }
        if (mealType == MealType.EATING_OUT) {
            return records
                .filter { record -> record.mealType == MealType.LUNCH || record.mealType == MealType.DINNER }
                .singleOrNull()
        }
        return null
    }

    private fun validateAppendTarget(
        target: MealRecord,
        recordMealType: MealType,
        nowMillis: Long,
        zoneId: ZoneId,
    ) {
        val isEatingOutAppend = recordMealType == MealType.EATING_OUT &&
            (target.mealType == MealType.LUNCH || target.mealType == MealType.DINNER)
        if (!target.mealType.isSameQuickRecordMealType(recordMealType) && !isEatingOutAppend) {
            throw RecordAppendTargetException()
        }
        val (startInclusive, endInclusive) = TimeRangeUtils.todayRange(nowMillis, zoneId)
        if (target.eatenAt !in startInclusive..endInclusive) {
            throw RecordAppendTargetException()
        }
    }

    private fun MealType.isSameQuickRecordMealType(other: MealType): Boolean {
        if (this == other) return true
        return this.isSnackType() && other.isSnackType()
    }

    private fun MealType.isSnackType(): Boolean {
        return this == MealType.FREE_SNACK || this == MealType.SNACK
    }
}
