package com.gonec009.meshizandaka.ui.template

import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal

/** 表示だけを集約する。元の各食の項目・栄養値は変更しない。 */
internal fun commonPreMealDrinkItems(meals: List<DrivePlanMeal>): List<DrivePlanItem> {
    val groups = linkedMapOf<Triple<String, String?, Int>, MutableList<Pair<DrivePlanMeal, DrivePlanItem>>>()
    meals.forEach { meal ->
        val occurrences = mutableMapOf<Pair<String, String?>, Int>()
        meal.items.filter { it.isPreMealDrink }.forEach { item ->
            val identity = item.name to item.imageContentHash
            val occurrence = occurrences.getOrDefault(identity, 0)
            occurrences[identity] = occurrence + 1
            groups.getOrPut(Triple(item.name, item.imageContentHash, occurrence)) { mutableListOf() }
                .add(meal to item)
        }
    }
    return groups.values.map { entries ->
        val first = entries.first().second
        val amounts = entries.groupBy { (_, item) -> item.amountLabel }
        val commonToAllMeals = entries.map { it.first.slot }.toSet() == meals.map { it.slot }.toSet()
        val amountLabel = if (amounts.size == 1 && commonToAllMeals) {
            first.amountLabel
        } else {
            amounts.entries.joinToString(" / ") { (amount, portions) ->
                val labels = portions.joinToString("・") { (meal, _) ->
                    meal.label.removeSuffix("のテンプレート")
                }
                "$labels: $amount"
            }
        }
        first.copy(amountLabel = amountLabel)
    }
}
