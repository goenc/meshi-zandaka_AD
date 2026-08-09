package com.gonec009.meshizandaka.ui.quickrecord

import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickRecordViewModelTest {
    @Test
    fun 外食カード選択中は通常テンプレートを保存対象にしない() {
        val template = MealTemplate(
            name = "昼食S",
            mealType = MealType.LUNCH,
            baseCalories = 500,
            proteinG = 20.0,
            fatG = 10.0,
            carbG = 50.0,
            isSpecial = false,
            comparisonTemplateId = null,
            weeklyLimitCount = null,
            monthlyLimitCount = null,
            memo = "",
        )
        val state = QuickRecordUiState(
            selectedTemplate = template,
            selectedDriveEatingOutCard = QuickRecordDriveCard(
                id = "samurai-mac",
                mealLabel = "外食",
                name = "サムライマック",
                amountLabel = "1 個",
                imagePath = null,
                calories = 600,
                proteinG = 25.0,
                fatG = 20.0,
                carbG = 70.0,
            ),
        )

        assertNull(state.templateForSave())
    }

    @Test
    fun 外食カード未選択なら通常テンプレートを保存対象にする() {
        val template = MealTemplate(
            name = "昼食S",
            mealType = MealType.LUNCH,
            baseCalories = 500,
            proteinG = 20.0,
            fatG = 10.0,
            carbG = 50.0,
            isSpecial = false,
            comparisonTemplateId = null,
            weeklyLimitCount = null,
            monthlyLimitCount = null,
            memo = "",
        )

        assertEquals(
            template,
            QuickRecordUiState(selectedTemplate = template).templateForSave(),
        )
    }

    @Test
    fun 外食カードの材料を個別登録オプションへ変換する() {
        val card = QuickRecordDriveCard(
            id = "samurai-mac",
            mealLabel = "外食",
            name = "サムライマック",
            amountLabel = "1 個",
            imagePath = null,
            calories = 600,
            proteinG = 25.0,
            fatG = 20.0,
            carbG = 70.0,
            items = listOf(
                QuickRecordDriveItem(
                    id = "bun",
                    name = "バンズ",
                    amountLabel = "1 個",
                    calories = 180,
                    proteinG = 5.0,
                    fatG = 2.0,
                    carbG = 30.0,
                ),
                QuickRecordDriveItem(
                    id = "patty",
                    name = "パティ",
                    amountLabel = "1 個",
                    calories = 420,
                    proteinG = 20.0,
                    fatG = 18.0,
                    carbG = 10.0,
                ),
            ),
        )

        val options = card.toRecordOptions()

        assertEquals(listOf("バンズ", "パティ"), options.map { it.optionNameSnapshot })
        assertEquals(listOf(180, 420), options.map { it.calorieDelta })
        assertEquals(listOf("サムライマック", "サムライマック"), options.map { it.optionGroupNameSnapshot })
    }

    @Test
    fun テンプレートなしの手入力は名前があれば保存可能() {
        assertEquals(
            false,
            QuickRecordUiState().canSave(),
        )
        assertEquals(
            true,
            QuickRecordUiState(
                templateName = "自作昼食",
                totalCalories = "500",
                proteinG = "25",
                fatG = "15",
                carbG = "50",
            ).canSave(),
        )
    }
}
