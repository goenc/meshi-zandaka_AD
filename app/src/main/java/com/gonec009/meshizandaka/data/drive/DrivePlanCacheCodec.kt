package com.gonec009.meshizandaka.data.drive

import org.json.JSONArray
import org.json.JSONObject

internal object DrivePlanCacheCodec {
    fun encode(catalog: DrivePlanCatalog): String {
        val root = JSONObject()
            .putNullable("preferredPlanId", catalog.preferredPlanId)
            .put("plans", JSONArray())
        val plans = root.getJSONArray("plans")

        catalog.plans.forEach { plan ->
            val planJson = JSONObject()
                .put("id", plan.id)
                .put("name", plan.name)
                .putNullable("targetDate", plan.targetDate)
                .put("memo", plan.memo)
                .put("isFavorite", plan.isFavorite)
                .put("displayOrder", plan.displayOrder)
                .put("updatedUtcTicks", plan.updatedUtcTicks)
                .put("meals", JSONArray())
            val meals = planJson.getJSONArray("meals")

            plan.meals.forEach { meal ->
                val mealJson = JSONObject()
                    .put("slot", meal.slot)
                    .put("label", meal.label)
                    .put("name", meal.name)
                    .put("memo", meal.memo)
                    .putNullable("imageContentHash", meal.imageContentHash)
                    .put("totalCalories", meal.totalCalories)
                    .put("proteinG", meal.proteinG)
                    .put("fatG", meal.fatG)
                    .put("carbG", meal.carbG)
                    .put("nutritionDataAvailable", meal.nutritionDataAvailable)
                    .put("items", JSONArray())
                val items = mealJson.getJSONArray("items")

                meal.items.forEach { item ->
                    items.put(
                        JSONObject()
                            .put("name", item.name)
                            .put("amountLabel", item.amountLabel)
                            .put("isMainDish", item.isMainDish)
                            .putNullable("imageContentHash", item.imageContentHash)
                            .put("calories", item.calories)
                            .put("proteinG", item.proteinG)
                            .put("fatG", item.fatG)
                            .put("carbG", item.carbG),
                    )
                }
                meals.put(mealJson)
            }
            plans.put(planJson)
        }
        return root.toString()
    }

    fun decode(value: String): DrivePlanCatalog {
        val root = JSONObject(value)
        val plansJson = root.optJSONArray("plans") ?: error("保存済みプランデータが不正です。")
        val plans = buildList {
            for (planIndex in 0 until plansJson.length()) {
                val planJson = plansJson.optJSONObject(planIndex) ?: continue
                val id = planJson.stringOrNull("id") ?: continue
                val mealsJson = planJson.optJSONArray("meals") ?: JSONArray()
                val meals = buildList {
                    for (mealIndex in 0 until mealsJson.length()) {
                        val mealJson = mealsJson.optJSONObject(mealIndex) ?: continue
                        val slot = mealJson.optInt("slot", mealIndex)
                        val itemsJson = mealJson.optJSONArray("items") ?: JSONArray()
                        val items = buildList {
                            for (itemIndex in 0 until itemsJson.length()) {
                                val itemJson = itemsJson.optJSONObject(itemIndex) ?: continue
                                add(
                                    DrivePlanItem(
                                        name = itemJson.stringOrNull("name").orEmpty().ifBlank { "食品" },
                                        amountLabel = itemJson.stringOrNull("amountLabel").orEmpty(),
                                        isMainDish = itemJson.optBoolean("isMainDish", false),
                                        imageContentHash = itemJson.stringOrNull("imageContentHash"),
                                        calories = itemJson.optInt("calories", 0),
                                        proteinG = itemJson.optDouble("proteinG", 0.0),
                                        fatG = itemJson.optDouble("fatG", 0.0),
                                        carbG = itemJson.optDouble("carbG", 0.0),
                                    ),
                                )
                            }
                        }
                        add(
                            DrivePlanMeal(
                                slot = slot,
                                label = mealJson.stringOrNull("label") ?: drivePlanMealLabel(slot),
                                name = mealJson.stringOrNull("name").orEmpty().ifBlank { "未設定" },
                                memo = mealJson.stringOrNull("memo").orEmpty(),
                                imageContentHash = mealJson.stringOrNull("imageContentHash"),
                                items = items,
                                totalCalories = mealJson.optInt("totalCalories", 0),
                                proteinG = mealJson.optDouble("proteinG", 0.0),
                                fatG = mealJson.optDouble("fatG", 0.0),
                                carbG = mealJson.optDouble("carbG", 0.0),
                                nutritionDataAvailable = mealJson.optBoolean("nutritionDataAvailable", false),
                            ),
                        )
                    }
                }
                add(
                    DrivePlan(
                        id = id,
                        name = planJson.stringOrNull("name").orEmpty().ifBlank { "名称未設定のプラン" },
                        targetDate = planJson.stringOrNull("targetDate"),
                        memo = planJson.stringOrNull("memo").orEmpty(),
                        isFavorite = planJson.optBoolean("isFavorite", false),
                        displayOrder = planJson.optInt("displayOrder", 0),
                        updatedUtcTicks = planJson.optLong("updatedUtcTicks", 0L),
                        meals = meals,
                    ),
                )
            }
        }
        return DrivePlanCatalog(
            plans = plans,
            preferredPlanId = root.stringOrNull("preferredPlanId"),
        )
    }

    private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
