package com.gonec009.meshizandaka.data.repository

import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealRecordRepositoryTest {
    @Test
    fun 外食カード項目を順番に全削除しても編集済みグループを保持する() {
        val burger = option(id = 10L, name = "バーガー", calories = 400)
        val fries = option(id = 11L, name = "ポテト", calories = 200)
        val record = MealRecord(
            id = 1L,
            eatenAt = 0L,
            mealType = MealType.LUNCH,
            templateId = 1L,
            templateNameSnapshot = "昼食 / 外食カード",
            totalCalories = 900,
            proteinG = 30.0,
            fatG = 20.0,
            carbG = 100.0,
            isSpecial = false,
            specialDeltaCalories = 0,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
            selectedOptions = listOf(burger, fries),
        )

        val updated = record.withoutOption(burger).withoutOption(fries)

        assertEquals(emptyList<MealRecordOption>(), updated.selectedOptions)
        assertEquals(setOf("外食カード"), updated.editedOptionGroupNames)
        assertEquals(300, updated.totalCalories)
        assertFalse(updated.shouldDeleteAfterItemRemoval())
    }

    @Test
    fun 項目削除後にゼロカロリーなら残存選択肢を確認せず削除対象にする() {
        val option = option(id = 10L, name = "外食メニュー", calories = 600)
        val zeroCalorieOption = option(id = 11L, name = "ゼロカロリーメニュー", calories = 0)
        val record = MealRecord(
            id = 1L,
            eatenAt = 0L,
            mealType = MealType.FREE_SNACK,
            templateId = null,
            templateNameSnapshot = "外食メニュー",
            totalCalories = 600,
            proteinG = 20.0,
            fatG = 10.0,
            carbG = 50.0,
            isSpecial = false,
            specialDeltaCalories = 0,
            sourceType = SourceType.QUICK_BUTTON,
            memo = "",
            selectedOptions = listOf(option, zeroCalorieOption),
        )

        val updated = record.withoutOption(option)

        assertEquals(0, updated.totalCalories)
        assertEquals(listOf(zeroCalorieOption), updated.selectedOptions)
        assertTrue(updated.shouldDeleteAfterItemRemoval())
    }

    private fun option(id: Long, name: String, calories: Int): MealRecordOption = MealRecordOption(
        id = id,
        mealRecordId = 1L,
        optionGroupNameSnapshot = "外食カード",
        optionNameSnapshot = name,
        calorieDelta = calories,
        proteinDeltaG = 0.0,
        fatDeltaG = 0.0,
        carbDeltaG = 0.0,
    )
}
