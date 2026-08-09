package com.gonec009.meshizandaka.data.drive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivePlanModelsTest {
    @Test
    fun 主菜候補は選択中の項目だけ栄養計算へ含める() {
        val regularItem = DrivePlanItem(
            name = "白米",
            amountLabel = "110 g",
            isMainDish = false,
            isMainDishCandidate = false,
        )
        val selectedMainDish = DrivePlanItem(
            name = "鶏むね肉",
            amountLabel = "100 g",
            isMainDish = true,
            isMainDishCandidate = true,
        )
        val unselectedMainDish = DrivePlanItem(
            name = "鮭",
            amountLabel = "100 g",
            isMainDish = false,
            isMainDishCandidate = true,
        )

        assertTrue(regularItem.isIncludedInMealNutrition())
        assertTrue(selectedMainDish.isIncludedInMealNutrition())
        assertFalse(unselectedMainDish.isIncludedInMealNutrition())
    }
}
