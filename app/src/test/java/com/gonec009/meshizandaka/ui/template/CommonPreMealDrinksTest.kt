package com.gonec009.meshizandaka.ui.template

import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
import org.junit.Assert.assertEquals
import org.junit.Test

class CommonPreMealDrinksTest {
    @Test
    fun 三食にある同じドリンクを一組へまとめ元の各食は保持する() {
        val meals = listOf(meal(0), meal(2), meal(3))
        val originals = meals.map { it.copy(items = it.items.toList()) }
        val items = commonPreMealDrinkItems(meals)
        assertEquals(listOf("イヌリン", "トマト飲料"), items.map { it.name })
        assertEquals(listOf("5 g", "30 ml"), items.map { it.amountLabel })
        assertEquals(originals, meals)
        assertEquals(300, meals.sumOf { it.totalCalories })
    }

    @Test
    fun 食事で数量が違う場合も一行にまとめ数量差を残す() {
        val items = commonPreMealDrinkItems(listOf(meal(0), meal(2), meal(3, tomatoAmount = "100 ml")))
        assertEquals(2, items.size)
        assertEquals("朝・昼: 30 ml / 夜: 100 ml", items[1].amountLabel)
    }

    @Test
    fun 一部の食事だけの項目は対象の食事を表示する() {
        val dinner = meal(3).let { it.copy(items = it.items.filterNot { item -> item.name == "トマト飲料" }) }
        val items = commonPreMealDrinkItems(listOf(meal(0), meal(2), dinner))
        assertEquals("朝・昼: 30 ml", items[1].amountLabel)
    }

    @Test
    fun 同じ食事内の重複項目は勝手に削除しない() {
        val breakfast = meal(0).let { it.copy(items = it.items + it.items.first()) }
        val items = commonPreMealDrinkItems(listOf(breakfast, meal(2), meal(3)))
        assertEquals(3, items.size)
        assertEquals("朝: 5 g", items.last().amountLabel)
    }

    private fun meal(slot: Int, tomatoAmount: String = "30 ml"): DrivePlanMeal = DrivePlanMeal(
        slot = slot,
        label = when (slot) { 0 -> "朝のテンプレート"; 2 -> "昼のテンプレート"; else -> "夜のテンプレート" },
        name = "食事",
        memo = "",
        totalCalories = 100,
        items = listOf(
            DrivePlanItem(name = "イヌリン", amountLabel = "5 g", isMainDish = false, isPreMealDrink = true, id = "$slot-inulin"),
            DrivePlanItem(name = "トマト飲料", amountLabel = tomatoAmount, isMainDish = false, isPreMealDrink = true, id = "$slot-tomato"),
            DrivePlanItem(name = "白米", amountLabel = "110 g", isMainDish = false),
        ),
    )
}
