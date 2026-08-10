package com.gonec009.meshizandaka.ui.home

import com.gonec009.meshizandaka.data.drive.DriveExternalCard
import com.gonec009.meshizandaka.data.drive.DriveFood
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
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

    @Test
    fun 連結された写真の表示名は最後に追加されたクイック記録名にする() {
        assertEquals("サムライマック", recordPhotoLabel("昼食S / サムライマック"))
    }

    @Test
    fun 外食カードに食品を追加した記録でも外食カードを判定する() {
        val card = DriveExternalCard(
            id = "samurai-mac",
            name = "サムライマック",
        )
        val record = MealRecord(
            eatenAt = 0L,
            mealType = MealType.LUNCH,
            templateId = null,
            templateNameSnapshot = "サムライマック / サバ",
            totalCalories = 675,
            proteinG = 50.0,
            fatG = 70.0,
            carbG = 80.0,
            isSpecial = false,
            specialDeltaCalories = 0,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
        )

        assertEquals(card, record.externalCard(listOf(card)))
    }

    @Test
    fun 食品記録から食品マスターの画像情報を解決する() {
        val food = DriveFood(
            id = "saba",
            name = "サバ",
            mealCategory = 0,
            amountLabel = "100 g",
            imagePath = "/cache/saba.jpg",
        )
        val option = MealRecordOption(
            id = 10L,
            optionGroupNameSnapshot = "食品",
            optionNameSnapshot = "サバ",
            calorieDelta = 158,
            proteinDeltaG = 20.0,
            fatDeltaG = 8.0,
            carbDeltaG = 0.0,
        )

        assertEquals(food, foodForRecordOption(option, listOf(food)))
    }
}
