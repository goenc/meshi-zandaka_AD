package com.gonec009.meshizandaka.data.drive

import org.json.JSONArray
import org.json.JSONObject

internal object DrivePlanCacheCodec {
    fun encode(catalog: DrivePlanCatalog): String {
        val root = JSONObject()
            .putNullable("preferredPlanId", catalog.preferredPlanId)
            .put("plans", JSONArray())
            .put("externalCards", JSONArray())
            .put("foods", JSONArray())
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
                            .putNullable("id", item.id)
                            .put("name", item.name)
                            .put("amountLabel", item.amountLabel)
                            .put("isMainDish", item.isMainDish)
                            .put("isMainDishCandidate", item.isMainDishCandidate)
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
        val externalCards = root.getJSONArray("externalCards")
        catalog.externalCards.forEach { card ->
            externalCards.put(
                JSONObject()
                    .put("id", card.id)
                    .put("name", card.name)
                    .putNullable("storeName", card.storeName)
                    .putNullable("tabName", card.tabName)
                    .put("amountLabel", card.amountLabel)
                    .put("memo", card.memo)
                    .putNullable("imageContentHash", card.imageContentHash)
                    .putNullable("imagePath", card.imagePath)
                    .put("calories", card.calories)
                    .put("proteinG", card.proteinG)
                    .put("fatG", card.fatG)
                    .put("carbG", card.carbG)
                    .put("items", JSONArray().apply {
                        card.items.forEach { item ->
                            put(
                                JSONObject()
                                    .putNullable("id", item.id)
                                    .put("name", item.name)
                                    .put("amountLabel", item.amountLabel)
                                    .put("isMainDish", item.isMainDish)
                                    .put("isMainDishCandidate", item.isMainDishCandidate)
                                    .putNullable("imageContentHash", item.imageContentHash)
                                    .putNullable("imagePath", item.imagePath)
                                    .put("calories", item.calories)
                                    .put("proteinG", item.proteinG)
                                    .put("fatG", item.fatG)
                                    .put("carbG", item.carbG),
                            )
                        }
                    }),
            )
        }
        val foods = root.getJSONArray("foods")
        catalog.foods.forEach { food ->
            foods.put(
                JSONObject()
                    .put("id", food.id)
                    .put("name", food.name)
                    .put("mealCategory", food.mealCategory)
                    .put("amountLabel", food.amountLabel)
                    .putNullable("imageContentHash", food.imageContentHash)
                    .putNullable("imagePath", food.imagePath)
                    .put("calories", food.calories)
                    .put("proteinG", food.proteinG)
                    .put("fatG", food.fatG)
                    .put("carbG", food.carbG),
            )
        }
        return root.toString()
    }

    fun decode(value: String): DrivePlanCatalog {
        val root = JSONObject(value)
        val plansJson = root.optJSONArray("plans") ?: error("保存済みプランデータが不正です。")
        val externalCardsJson = root.optJSONArray("externalCards") ?: JSONArray()
        val foodsJson = root.optJSONArray("foods") ?: JSONArray()
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
                                val isMainDish = itemJson.optBoolean("isMainDish", false)
                                add(
                                    DrivePlanItem(
                                        name = itemJson.stringOrNull("name").orEmpty().ifBlank { "食品" },
                                        amountLabel = itemJson.stringOrNull("amountLabel").orEmpty(),
                                        isMainDish = isMainDish,
                                        isMainDishCandidate = itemJson.optBoolean("isMainDishCandidate", false) || isMainDish,
                                        imageContentHash = itemJson.stringOrNull("imageContentHash"),
                                        calories = itemJson.optInt("calories", 0),
                                        proteinG = itemJson.optDouble("proteinG", 0.0),
                                        fatG = itemJson.optDouble("fatG", 0.0),
                                        carbG = itemJson.optDouble("carbG", 0.0),
                                        id = itemJson.stringOrNull("id"),
                                    ),
                                )
                            }
                        }
                        add(
                            DrivePlanMeal(
                                slot = slot,
                                label = drivePlanMealLabel(slot),
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
        val externalCards = buildList {
            for (index in 0 until externalCardsJson.length()) {
                val cardJson = externalCardsJson.optJSONObject(index) ?: continue
                val id = cardJson.stringOrNull("id") ?: continue
                val itemsJson = cardJson.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (itemIndex in 0 until itemsJson.length()) {
                        val itemJson = itemsJson.optJSONObject(itemIndex) ?: continue
                        val isMainDish = itemJson.optBoolean("isMainDish", false)
                        add(
                            DrivePlanItem(
                                name = itemJson.stringOrNull("name").orEmpty().ifBlank { "食品" },
                                amountLabel = itemJson.stringOrNull("amountLabel").orEmpty(),
                                isMainDish = isMainDish,
                                isMainDishCandidate = itemJson.optBoolean("isMainDishCandidate", false) || isMainDish,
                                imageContentHash = itemJson.stringOrNull("imageContentHash"),
                                imagePath = itemJson.stringOrNull("imagePath"),
                                calories = itemJson.optInt("calories", 0),
                                proteinG = itemJson.optDouble("proteinG", 0.0),
                                fatG = itemJson.optDouble("fatG", 0.0),
                                carbG = itemJson.optDouble("carbG", 0.0),
                                id = itemJson.stringOrNull("id"),
                            ),
                        )
                    }
                }
                add(
                    DriveExternalCard(
                        id = id,
                        name = cardJson.stringOrNull("name").orEmpty().ifBlank { "外食カード" },
                        storeName = cardJson.stringOrNull("storeName"),
                        tabName = cardJson.stringOrNull("tabName"),
                        amountLabel = cardJson.stringOrNull("amountLabel").orEmpty().ifBlank { "1 個" },
                        memo = cardJson.stringOrNull("memo").orEmpty(),
                        imageContentHash = cardJson.stringOrNull("imageContentHash"),
                        imagePath = cardJson.stringOrNull("imagePath"),
                        calories = cardJson.optInt("calories", 0),
                        proteinG = cardJson.optDouble("proteinG", 0.0),
                        fatG = cardJson.optDouble("fatG", 0.0),
                        carbG = cardJson.optDouble("carbG", 0.0),
                        items = items,
                    ),
                )
            }
        }
        val foods = buildList {
            for (index in 0 until foodsJson.length()) {
                val foodJson = foodsJson.optJSONObject(index) ?: continue
                val id = foodJson.stringOrNull("id") ?: continue
                add(
                    DriveFood(
                        id = id,
                        name = foodJson.stringOrNull("name").orEmpty().ifBlank { "食品" },
                        mealCategory = foodJson.optInt("mealCategory", 4)
                            .takeIf { it in 0..4 } ?: 4,
                        amountLabel = foodJson.stringOrNull("amountLabel").orEmpty(),
                        imageContentHash = foodJson.stringOrNull("imageContentHash"),
                        imagePath = foodJson.stringOrNull("imagePath"),
                        calories = foodJson.optInt("calories", 0),
                        proteinG = foodJson.optDouble("proteinG", 0.0),
                        fatG = foodJson.optDouble("fatG", 0.0),
                        carbG = foodJson.optDouble("carbG", 0.0),
                    ),
                )
            }
        }
        return DrivePlanCatalog(
            plans = plans,
            preferredPlanId = root.stringOrNull("preferredPlanId"),
            externalCards = externalCards,
            foods = foods,
        )
    }

    private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
