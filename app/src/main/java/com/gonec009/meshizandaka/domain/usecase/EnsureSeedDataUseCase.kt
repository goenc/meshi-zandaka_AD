package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository
import com.gonec009.meshizandaka.data.repository.SettingsRepository
import com.gonec009.meshizandaka.domain.model.AppSettings
import kotlinx.coroutines.flow.first

class EnsureSeedDataUseCase(
    private val templateRepository: MealTemplateRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        if (templateRepository.countTemplates() > 0) {
            return
        }

        val templates = listOf(
            MealTemplateEntity(
                id = 1,
                name = "朝セット",
                mealType = "BREAKFAST",
                baseCalories = 400,
                proteinG = 20,
                fatG = 10,
                carbG = 45,
                isSpecial = false,
                comparisonTemplateId = null,
                weeklyLimitCount = null,
                monthlyLimitCount = null,
                memo = "朝の固定セット",
            ),
            MealTemplateEntity(
                id = 2,
                name = "昼食セット",
                mealType = "LUNCH",
                baseCalories = 180,
                proteinG = 6,
                fatG = 4,
                carbG = 20,
                isSpecial = false,
                comparisonTemplateId = null,
                weeklyLimitCount = null,
                monthlyLimitCount = null,
                memo = "昼の標準セット",
            ),
            MealTemplateEntity(
                id = 3,
                name = "夕食セット",
                mealType = "DINNER",
                baseCalories = 220,
                proteinG = 8,
                fatG = 5,
                carbG = 22,
                isSpecial = false,
                comparisonTemplateId = null,
                weeklyLimitCount = null,
                monthlyLimitCount = null,
                memo = "夜の標準セット",
            ),
            MealTemplateEntity(
                id = 4,
                name = "二郎系",
                mealType = "EATING_OUT",
                baseCalories = 1500,
                proteinG = 55,
                fatG = 65,
                carbG = 160,
                isSpecial = true,
                comparisonTemplateId = 2,
                weeklyLimitCount = null,
                monthlyLimitCount = 1,
                memo = "特別メシ",
            ),
            MealTemplateEntity(
                id = 5,
                name = "マック作業食",
                mealType = "EATING_OUT",
                baseCalories = 1100,
                proteinG = 28,
                fatG = 48,
                carbG = 132,
                isSpecial = true,
                comparisonTemplateId = 1,
                weeklyLimitCount = null,
                monthlyLimitCount = 4,
                memo = "特別メシ",
            ),
            MealTemplateEntity(
                id = 6,
                name = "その他外食",
                mealType = "EATING_OUT",
                baseCalories = 950,
                proteinG = 30,
                fatG = 35,
                carbG = 105,
                isSpecial = true,
                comparisonTemplateId = 2,
                weeklyLimitCount = null,
                monthlyLimitCount = 4,
                memo = "特別メシ",
            ),
        )

        val groupsByTemplateIndex = mapOf(
            0 to listOf(
                TemplateOptionGroupEntity(templateId = 0, name = "主菜", selectionType = "SINGLE"),
                TemplateOptionGroupEntity(templateId = 0, name = "主食量", selectionType = "SINGLE"),
            ),
            1 to listOf(
                TemplateOptionGroupEntity(templateId = 0, name = "主菜", selectionType = "SINGLE"),
                TemplateOptionGroupEntity(templateId = 0, name = "主食量", selectionType = "SINGLE"),
            ),
        )

        val mainDishOptions = listOf(
            TemplateOptionEntity(groupId = 0, name = "鶏胸肉", calorieDelta = 160, proteinDeltaG = 32, fatDeltaG = 3, carbDeltaG = 0, sortOrder = 0),
            TemplateOptionEntity(groupId = 0, name = "鮭", calorieDelta = 220, proteinDeltaG = 22, fatDeltaG = 14, carbDeltaG = 0, sortOrder = 1),
            TemplateOptionEntity(groupId = 0, name = "豚肉", calorieDelta = 250, proteinDeltaG = 20, fatDeltaG = 18, carbDeltaG = 0, sortOrder = 2),
            TemplateOptionEntity(groupId = 0, name = "牛肉", calorieDelta = 300, proteinDeltaG = 20, fatDeltaG = 24, carbDeltaG = 0, sortOrder = 3),
            TemplateOptionEntity(groupId = 0, name = "納豆", calorieDelta = 100, proteinDeltaG = 8, fatDeltaG = 5, carbDeltaG = 6, sortOrder = 4),
            TemplateOptionEntity(groupId = 0, name = "卵", calorieDelta = 80, proteinDeltaG = 7, fatDeltaG = 6, carbDeltaG = 0, sortOrder = 5),
        )
        val riceOptions = listOf(
            TemplateOptionEntity(groupId = 0, name = "白米80g", calorieDelta = 130, proteinDeltaG = 2, fatDeltaG = 0, carbDeltaG = 29, sortOrder = 0),
            TemplateOptionEntity(groupId = 0, name = "白米110g", calorieDelta = 180, proteinDeltaG = 3, fatDeltaG = 0, carbDeltaG = 40, sortOrder = 1),
            TemplateOptionEntity(groupId = 0, name = "白米150g", calorieDelta = 250, proteinDeltaG = 4, fatDeltaG = 0, carbDeltaG = 55, sortOrder = 2),
        )
        val optionsByGroupIndex = mapOf(
            (0 to 0) to mainDishOptions,
            (0 to 1) to riceOptions,
            (1 to 0) to mainDishOptions,
            (1 to 1) to riceOptions,
        )

        val insertedIds = templateRepository.insertSeedData(templates, groupsByTemplateIndex, optionsByGroupIndex)
        val currentSettings = settingsRepository.settingsFlow.first()
        settingsRepository.updateSettings(
            currentSettings.copy(
                defaultBreakfastTemplateId = insertedIds[0],
                defaultLunchTemplateId = insertedIds[1],
                defaultDinnerTemplateId = insertedIds[2],
            ),
        )
    }
}
