package com.gonec009.meshizandaka.data.drive

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivePreMealDrinkTest {
    @Test
    fun PascalCaseとcamelCaseの食前ドリンク区分を読み取る() {
        for (key in listOf("IsPreMealDrink", "isPreMealDrink")) {
            val item = catalog(key = key).plans.single().meals.first().items.first()
            assertTrue(item.isPreMealDrink)
            assertEquals("50 ml", item.amountLabel)
            assertEquals(50, item.calories)
            assertEquals(5.0, item.proteinG, 0.001)
            assertEquals(1.0, item.fatG, 0.001)
            assertEquals(7.5, item.carbG, 0.001)
        }
    }

    @Test
    fun 区分がない旧同期データと明示falseは通常項目として扱う() {
        assertFalse(catalog(key = null).plans.single().meals.first().items.first().isPreMealDrink)
        assertFalse(catalog(flag = false).plans.single().meals.first().items.first().isPreMealDrink)
    }

    @Test
    fun 食品とレシピの食前ドリンクは数量に応じて一度だけ集計する() {
        for (recipe in listOf(false, true)) {
            val meal = catalog(recipe = recipe).plans.single().meals.first()
            assertEquals(listOf("drink", "main", "alternate", "regular"), meal.items.map { it.id })
            assertEquals(350, meal.totalCalories)
            assertEquals(35.0, meal.proteinG, 0.001)
            assertEquals(7.0, meal.fatG, 0.001)
            assertEquals(52.5, meal.carbG, 0.001)
            assertEquals(50, meal.items.first().calories)
            assertEquals(if (recipe) "50 g" else "50 ml", meal.items.first().amountLabel)
            assertEquals(300, meal.items[2].calories)
            assertTrue(meal.items.first().isIncludedInMealNutrition())
            assertFalse(meal.items[2].isIncludedInMealNutrition())
            assertTrue(meal.nutritionDataAvailable)
        }
    }

    @Test
    fun 食前ドリンク区分は主菜候補の選択条件と記録キーを変えない() {
        val drink = catalog().plans.single().meals.first().items.first()
        val candidate = drink.copy(isMainDishCandidate = true)
        assertFalse(candidate.isIncludedInMealNutrition())
        assertTrue(candidate.copy(isMainDish = true).isIncludedInMealNutrition())
        assertEquals(drink.recordKey(0), drink.copy(isPreMealDrink = false).recordKey(0))
    }

    @Test
    fun キャッシュ往復で区分と数量と栄養値を保持する() {
        val original = catalog()
        val drink = original.plans.single().meals.first().items.first()
        val withCard = original.copy(
            externalCards = listOf(DriveExternalCard(id = "card", name = "カード", items = listOf(drink))),
        )
        val encoded = DrivePlanCacheCodec.encode(withCard)
        assertTrue(JSONObject(encoded).getJSONArray("plans").getJSONObject(0)
            .getJSONArray("meals").getJSONObject(0).getJSONArray("items")
            .getJSONObject(0).getBoolean("isPreMealDrink"))
        assertEquals(withCard, DrivePlanCacheCodec.decode(encoded))
    }

    @Test
    fun 区分がない旧キャッシュも数量と栄養値を保持して復元する() {
        val original = catalog(key = null)
        val root = JSONObject(DrivePlanCacheCodec.encode(original))
        val meals = root.getJSONArray("plans").getJSONObject(0).getJSONArray("meals")
        for (mealIndex in 0 until meals.length()) {
            val items = meals.getJSONObject(mealIndex).getJSONArray("items")
            for (itemIndex in 0 until items.length()) {
                items.getJSONObject(itemIndex).remove("isPreMealDrink")
            }
        }
        assertEquals(original, DrivePlanCacheCodec.decode(root.toString()))
    }

    private fun catalog(
        key: String? = "IsPreMealDrink",
        flag: Boolean = true,
        recipe: Boolean = false,
    ): DrivePlanCatalog {
        val rows = mutableListOf(
            row("DailyPlanEntity", "plan", JSONObject().put("Id", "plan").put("Name", "同期プラン")),
            row("DailyPlanMealSnapshotEntity", "meal", JSONObject()
                .put("Id", "meal").put("DailyPlanId", "plan").put("Slot", 0)
                .put("SelectedMainDishItemId", "main")),
        )
        for ((index, id) in listOf("drink", "main", "alternate", "regular", "disabled").withIndex()) {
            val item = JSONObject()
                .put("Id", id).put("DailyPlanMealSnapshotId", "meal")
                .put("IsEnabled", id != "disabled").put("DisplayOrder", index)
                .put("IsMainDishCandidate", id == "main" || id == "alternate")
                .put("ComponentType", if (recipe && id == "drink") 1 else 0)
                .put("StandardAmount", if (id == "drink") 50_000L else 100_000L)
                .put("StandardUnit", if (recipe && id == "drink") 0 else 1)
            if (id == "drink" && key != null) item.put(key, flag)
            rows += row("DailyPlanMealItemSnapshotEntity", id, item)
            val calories = when (id) {
                "main" -> 200
                "alternate" -> 300
                "disabled" -> 900
                else -> 100
            }
            val child = JSONObject().put("Name", id)
                .put("ReferenceAmount", 100_000L).put("ReferenceUnit", 1)
            if (recipe && id == "drink") {
                child.put("FinishedWeightMg", 100_000L)
                    .put("ExternalEnergyTenthsKcal", calories * 10)
                    .put("ExternalProteinMilligrams", calories * 100)
                    .put("ExternalFatMilligrams", calories * 20)
                    .put("ExternalCarbohydrateMilligrams", calories * 150)
                rows += row("DailyPlanRecipeSnapshotEntity", id, child)
            } else {
                child.put("EnergyTenthsKcal", calories * 10)
                    .put("ProteinMilligrams", calories * 100)
                    .put("FatMilligrams", calories * 20)
                    .put("CarbohydrateMilligrams", calories * 150)
                rows += row("DailyPlanFoodSnapshotEntity", id, child)
            }
        }
        return DrivePlanCatalogBuilder.build(rows)
    }

    private fun row(type: String, id: String, payload: JSONObject) = SnapshotRow(
        entityType = type,
        rowKey = id,
        payload = payload,
        revision = SnapshotRevision(1L, "pc", 1L, id),
    )
}
