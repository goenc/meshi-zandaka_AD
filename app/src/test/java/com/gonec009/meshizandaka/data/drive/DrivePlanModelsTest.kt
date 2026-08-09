package com.gonec009.meshizandaka.data.drive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivePlanModelsTest {
    @Test
    fun Drive再読込時は同じ画像ハッシュのキャッシュパスを保持する() {
        val previousPlan = DrivePlan(
            id = "plan",
            name = "プラン",
            targetDate = null,
            memo = "",
            isFavorite = false,
            displayOrder = 0,
            updatedUtcTicks = 0L,
            meals = listOf(
                DrivePlanMeal(
                    slot = 0,
                    label = "朝食",
                    name = "朝食",
                    memo = "",
                    imageContentHash = "meal-hash",
                    imagePath = "/cache/meal.jpg",
                    items = listOf(
                        DrivePlanItem(
                            name = "主菜",
                            amountLabel = "100 g",
                            isMainDish = true,
                            imageContentHash = "item-hash",
                            imagePath = "/cache/item.jpg",
                        ),
                    ),
                ),
            ),
        )
        val refreshedPlan = previousPlan.copy(
            meals = previousPlan.meals.map { meal ->
                meal.copy(
                    imagePath = null,
                    items = meal.items.map { item -> item.copy(imagePath = null) },
                )
            },
        )

        val preserved = DrivePlanCatalog(
            plans = listOf(refreshedPlan),
            preferredPlanId = "plan",
        ).preserveImagePathsFrom(
            DrivePlanState(plans = listOf(previousPlan)),
        )

        assertEquals("/cache/meal.jpg", preserved.plans.single().meals.single().imagePath)
        assertEquals("/cache/item.jpg", preserved.plans.single().meals.single().items.single().imagePath)
    }

    @Test
    fun Drive再読込時は画像ハッシュが変われば古いパスを保持しない() {
        val previousPlan = DrivePlan(
            id = "plan",
            name = "プラン",
            targetDate = null,
            memo = "",
            isFavorite = false,
            displayOrder = 0,
            updatedUtcTicks = 0L,
            meals = listOf(
                DrivePlanMeal(
                    slot = 0,
                    label = "朝食",
                    name = "朝食",
                    memo = "",
                    imageContentHash = "old-hash",
                    imagePath = "/cache/old.jpg",
                ),
            ),
        )
        val refreshed = previousPlan.copy(
            meals = previousPlan.meals.map { meal ->
                meal.copy(imageContentHash = "new-hash", imagePath = null)
            },
        )

        val preserved = DrivePlanCatalog(
            plans = listOf(refreshed),
            preferredPlanId = "plan",
        ).preserveImagePathsFrom(
            DrivePlanState(plans = listOf(previousPlan)),
        )

        assertNull(preserved.plans.single().meals.single().imagePath)
    }

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
