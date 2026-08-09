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
}
