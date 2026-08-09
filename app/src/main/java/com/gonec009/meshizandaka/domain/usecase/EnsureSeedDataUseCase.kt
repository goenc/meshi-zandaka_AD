package com.gonec009.meshizandaka.domain.usecase

import com.gonec009.meshizandaka.data.local.entity.MealTemplateEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionEntity
import com.gonec009.meshizandaka.data.local.entity.TemplateOptionGroupEntity
import com.gonec009.meshizandaka.data.repository.MealTemplateRepository

class EnsureSeedDataUseCase(
    private val templateRepository: MealTemplateRepository,
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
                shortcutRole = "BREAKFAST",
                baseCalories = 400,
                proteinG = 20.0,
                fatG = 10.0,
                carbG = 45.0,
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
                shortcutRole = "LUNCH",
                baseCalories = 180,
                proteinG = 6.0,
                fatG = 4.0,
                carbG = 20.0,
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
                shortcutRole = "DINNER",
                baseCalories = 220,
                proteinG = 8.0,
                fatG = 5.0,
                carbG = 22.0,
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
                shortcutRole = "NONE",
                baseCalories = 1500,
                proteinG = 55.0,
                fatG = 65.0,
                carbG = 160.0,
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
                shortcutRole = "NONE",
                baseCalories = 1100,
                proteinG = 28.0,
                fatG = 48.0,
                carbG = 132.0,
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
                shortcutRole = "NONE",
                baseCalories = 950,
                proteinG = 30.0,
                fatG = 35.0,
                carbG = 105.0,
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
            TemplateOptionEntity(groupId = 0, name = "鶏胸肉", calorieDelta = 160, proteinDeltaG = 32.0, fatDeltaG = 3.0, carbDeltaG = 0.0, sortOrder = 0),
            TemplateOptionEntity(groupId = 0, name = "鮭", calorieDelta = 220, proteinDeltaG = 22.0, fatDeltaG = 14.0, carbDeltaG = 0.0, sortOrder = 1),
            TemplateOptionEntity(groupId = 0, name = "豚肉", calorieDelta = 250, proteinDeltaG = 20.0, fatDeltaG = 18.0, carbDeltaG = 0.0, sortOrder = 2),
            TemplateOptionEntity(groupId = 0, name = "牛肉", calorieDelta = 300, proteinDeltaG = 20.0, fatDeltaG = 24.0, carbDeltaG = 0.0, sortOrder = 3),
            TemplateOptionEntity(groupId = 0, name = "納豆", calorieDelta = 100, proteinDeltaG = 8.0, fatDeltaG = 5.0, carbDeltaG = 6.0, sortOrder = 4),
            TemplateOptionEntity(groupId = 0, name = "卵", calorieDelta = 80, proteinDeltaG = 7.0, fatDeltaG = 6.0, carbDeltaG = 0.0, sortOrder = 5),
        )
        val riceOptions = listOf(
            TemplateOptionEntity(groupId = 0, name = "白米80g", calorieDelta = 130, proteinDeltaG = 2.0, fatDeltaG = 0.0, carbDeltaG = 29.0, sortOrder = 0),
            TemplateOptionEntity(groupId = 0, name = "白米110g", calorieDelta = 180, proteinDeltaG = 3.0, fatDeltaG = 0.0, carbDeltaG = 40.0, sortOrder = 1),
            TemplateOptionEntity(groupId = 0, name = "白米150g", calorieDelta = 250, proteinDeltaG = 4.0, fatDeltaG = 0.0, carbDeltaG = 55.0, sortOrder = 2),
        )
        val optionsByGroupIndex = mapOf(
            (0 to 0) to mainDishOptions,
            (0 to 1) to riceOptions,
            (1 to 0) to mainDishOptions,
            (1 to 1) to riceOptions,
        )

        templateRepository.insertSeedData(templates, groupsByTemplateIndex, optionsByGroupIndex)
    }
}
