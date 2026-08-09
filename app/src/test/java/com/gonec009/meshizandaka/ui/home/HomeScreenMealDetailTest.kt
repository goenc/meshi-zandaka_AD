package com.gonec009.meshizandaka.ui.home

import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeScreenMealDetailTest {
    private val lunchPlan = DrivePlan(
        id = "plan",
        name = "食事プラン",
        targetDate = null,
        memo = "",
        isFavorite = false,
        displayOrder = 0,
        updatedUtcTicks = 0L,
        meals = listOf(
            DrivePlanMeal(
                slot = 2,
                label = "昼食",
                name = "昼食S",
                memo = "",
            ),
        ),
    )

    @Test
    fun テンプレートなしの外食クイック記録へ昼食プランを紐付けない() {
        val record = MealRecord(
            eatenAt = 0L,
            mealType = MealType.LUNCH,
            templateId = null,
            templateNameSnapshot = "サムライマック",
            totalCalories = 600,
            proteinG = 25.0,
            fatG = 20.0,
            carbG = 70.0,
            isSpecial = false,
            specialDeltaCalories = 0,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "1 個",
        )

        assertNull(lunchPlan.mealForRecord(record))
    }

    @Test
    fun テンプレートありの昼食記録は昼食プランを表示する() {
        val record = MealRecord(
            eatenAt = 0L,
            mealType = MealType.LUNCH,
            templateId = -1002L,
            templateNameSnapshot = "昼食S",
            totalCalories = 600,
            proteinG = 25.0,
            fatG = 20.0,
            carbG = 70.0,
            isSpecial = false,
            specialDeltaCalories = 0,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
        )

        assertEquals("昼食S", lunchPlan.mealForRecord(record)?.name)
    }
}
