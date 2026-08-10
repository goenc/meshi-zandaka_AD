package com.gonec009.meshizandaka.data.drive

import java.math.BigDecimal
import java.util.Locale
import kotlin.math.roundToInt
import org.json.JSONObject

internal object DrivePlanCatalogBuilder {
    fun build(rows: Collection<SnapshotRow>): DrivePlanCatalog {
        val imageHashes = rows
            .asSequence()
            .filter { it.isEntity("ImageAssetEntity") }
            .mapNotNull { row ->
                val payload = row.payload ?: return@mapNotNull null
                val id = payload.stringOrNull("id") ?: row.rowKey
                val hash = payload.stringOrNull("sha256") ?: return@mapNotNull null
                idKey(id) to hash
            }
            .toMap()

        val storedRecipeRows = rows
            .asSequence()
            .filter { it.isEntity("RecipeEntity") }
            .mapNotNull { row ->
                val payload = row.payload ?: return@mapNotNull null
                idKey(payload.stringOrNull("id") ?: row.rowKey) to row
            }
            .toMap()
        val storedRecipeIngredientRows = groupRowsByField(
            rows.filter { it.isEntity("RecipeIngredientEntity") },
            "recipeId",
        )
        val storedFoodRows = rows
            .asSequence()
            .filter { it.isEntity("FoodEntity") }
            .mapNotNull { row ->
                val payload = row.payload ?: return@mapNotNull null
                idKey(payload.stringOrNull("id") ?: row.rowKey) to row
            }
            .toMap()
        val storedFoodPortionRows = groupRowsByField(
            rows.filter { it.isEntity("FoodPortionEntity") },
            "foodId",
        )

        val mealRows = rows.filter { it.isEntity("DailyPlanMealSnapshotEntity") }
        val itemRows = rows.filter { it.isEntity("DailyPlanMealItemSnapshotEntity") }
        val foodRows = rows.filter { it.isEntity("DailyPlanFoodSnapshotEntity") }
            .associateBy { idKey(it.rowKey) }
        val recipeRows = rows.filter { it.isEntity("DailyPlanRecipeSnapshotEntity") }
            .associateBy { idKey(it.rowKey) }
        val foodPortionRows = groupRowsByField(
            rows.filter { it.isEntity("DailyPlanFoodPortionSnapshotEntity") },
            "dailyPlanMealItemSnapshotId",
        )
        val recipeIngredientRows = groupRowsByField(
            rows.filter { it.isEntity("DailyPlanRecipeIngredientSnapshotEntity") },
            "dailyPlanRecipeSnapshotId",
        )
        val recipePortionRows = groupRowsByField(
            rows.filter { it.isEntity("DailyPlanRecipePortionSnapshotEntity") },
            "dailyPlanRecipeIngredientSnapshotId",
        )

        val plans = rows
            .asSequence()
            .filter { it.isEntity("DailyPlanEntity") }
            .mapNotNull { row ->
                val payload = row.payload ?: return@mapNotNull null
                if (payload.optBooleanIgnoreCase("isArchived", false)) return@mapNotNull null
                val id = payload.stringOrNull("id") ?: row.rowKey
                val planMealRows = mealRows
                    .asSequence()
                    .filter { mealRow ->
                        val meal = mealRow.payload ?: return@filter false
                        !meal.optBooleanIgnoreCase("isArchived", false) &&
                            idKey(meal.stringOrNull("dailyPlanId")) == idKey(id)
                    }
                    .toList()
                val meals = (0..5).map { slot ->
                    val mealRow = planMealRows
                        .filter { mealRow -> mealRow.payload?.optIntIgnoreCase("slot", -1) == slot }
                        .maxByOrNull { it.revision }
                    buildMeal(
                        slot = slot,
                        mealRow = mealRow,
                        itemRows = itemRows,
                        foodRows = foodRows,
                        recipeRows = recipeRows,
                        foodPortionRows = foodPortionRows,
                        recipeIngredientRows = recipeIngredientRows,
                        recipePortionRows = recipePortionRows,
                        imageHashes = imageHashes,
                    )
                }
                DrivePlan(
                    id = id,
                    name = payload.stringOrNull("name").orEmpty().ifBlank { "名称未設定のプラン" },
                    targetDate = payload.stringOrNull("targetDate"),
                    memo = payload.stringOrNull("memo").orEmpty(),
                    isFavorite = payload.optBooleanIgnoreCase("isFavorite", false),
                    displayOrder = payload.optIntIgnoreCase("displayOrder", 0),
                    updatedUtcTicks = payload.optLongIgnoreCase("updatedUtcTicks", 0L),
                    meals = meals,
                )
            }
            .sortedWith(compareBy<DrivePlan> { it.displayOrder }.thenBy { it.name }.thenBy { it.id })
            .toList()

        val preferredPlanId = rows
            .asSequence()
            .filter { it.isEntity("ComparisonBoardEntity") }
            .maxByOrNull { it.revision }
            ?.payload
            ?.stringOrNull("selectedPlanId")
            ?.let { preferredId -> plans.firstOrNull { it.id.equals(preferredId, ignoreCase = true) }?.id }

        val externalCards = buildExternalCards(
            recipeRows = storedRecipeRows,
            ingredientRows = storedRecipeIngredientRows,
            foodRows = storedFoodRows,
            foodPortionRows = storedFoodPortionRows,
            imageHashes = imageHashes,
        )
        val foods = buildFoods(
            foodRows = storedFoodRows,
            foodPortionRows = storedFoodPortionRows,
            imageHashes = imageHashes,
        )

        return DrivePlanCatalog(
            plans = plans,
            preferredPlanId = preferredPlanId,
            externalCards = externalCards,
            foods = foods,
        )
    }

    private fun buildFoods(
        foodRows: Map<String, SnapshotRow>,
        foodPortionRows: Map<String, List<SnapshotRow>>,
        imageHashes: Map<String, String>,
    ): List<DriveFood> = foodRows.values
        .asSequence()
        .mapNotNull { row ->
            val food = row.payload ?: return@mapNotNull null
            if (food.optBooleanIgnoreCase("isArchived", false)) return@mapNotNull null
            val id = food.stringOrNull("id") ?: row.rowKey
            val name = food.stringOrNull("displayName")
                ?: food.stringOrNull("name")
                ?: food.stringOrNull("officialName")
                ?: food.stringOrNull("foodName")
                ?: return@mapNotNull null
            val standardAmount = food.optLongIgnoreCase("standardAmount", 0L)
                .takeIf { it > 0L }
                ?: food.optLongIgnoreCase("referenceAmount", 0L)
            val standardUnit = food.optIntIgnoreCase("standardUnit", -1)
                .takeIf { it >= 0 }
                ?: food.optIntIgnoreCase("referenceUnit", -1)
            val standardCustomUnitName = food.stringOrNull("standardCustomUnitName")
                ?: food.stringOrNull("referenceCustomUnitName")
            val nutritionItem = JSONObject()
                .put("standardAmount", standardAmount)
                .put("standardUnit", standardUnit)
                .putNullable("standardCustomUnitName", standardCustomUnitName)
            val nutrition = nutritionForFood(
                food = food,
                item = nutritionItem,
                portionRows = foodPortionRows[idKey(id)].orEmpty(),
            )
            DriveFood(
                id = id,
                name = name,
                mealCategory = foodMealCategory(food),
                amountLabel = formatStoredAmount(
                    amount = standardAmount,
                    unit = standardUnit,
                    customUnitName = standardCustomUnitName,
                ),
                imageContentHash = food.stringOrNull("imageAssetId")
                    ?.let { imageHashes[idKey(it)] },
                calories = nutrition.calories.roundToInt(),
                proteinG = nutrition.proteinG,
                fatG = nutrition.fatG,
                carbG = nutrition.carbG,
            )
        }
        .sortedWith(compareBy<DriveFood> { it.mealCategory }.thenBy { it.name }.thenBy { it.id })
        .toList()

    private fun foodMealCategory(food: JSONObject): Int {
        val value = food.valueIgnoreCase("mealCategory")
        val category = when (value) {
            is Number -> value.toInt()
            is String -> {
                val normalized = value.lowercase(Locale.ROOT)
                    .substringAfterLast('.')
                    .replace("-", "")
                    .replace("_", "")
                value.toIntOrNull() ?: when (normalized) {
                    "staple", "mainstaple", "主食" -> 0
                    "sidedish", "副菜" -> 1
                    "maindish", "主菜" -> 2
                    "snack", "間食" -> 3
                    "other", "その他" -> 4
                    else -> 4
                }
            }
            else -> 4
        }
        return category.takeIf { it in 0..4 } ?: 4
    }

    private fun buildExternalCards(
        recipeRows: Map<String, SnapshotRow>,
        ingredientRows: Map<String, List<SnapshotRow>>,
        foodRows: Map<String, SnapshotRow>,
        foodPortionRows: Map<String, List<SnapshotRow>>,
        imageHashes: Map<String, String>,
    ): List<DriveExternalCard> = recipeRows.values
        .asSequence()
        .mapNotNull { row ->
            val recipe = row.payload ?: return@mapNotNull null
            if (!recipe.isEatingOutRecipe() || recipe.optBooleanIgnoreCase("isArchived", false)) {
                return@mapNotNull null
            }
            val id = recipe.stringOrNull("id") ?: row.rowKey
            val ingredients = ingredientRows[idKey(id)].orEmpty()
            if (ingredients.none { ingredient ->
                    ingredient.payload?.optBooleanIgnoreCase("isEnabled", true) == true
                }
            ) {
                return@mapNotNull null
            }
            val nutrition = nutritionForStoredRecipe(
                recipe = recipe,
                recipeRows = recipeRows,
                ingredientRows = ingredientRows,
                foodRows = foodRows,
                foodPortionRows = foodPortionRows,
                visiting = emptySet(),
            )
            DriveExternalCard(
                id = id,
                name = recipe.stringOrNull("name").orEmpty().ifBlank { "外食カード" },
                storeName = recipe.stringOrNull("externalStoreName"),
                tabName = recipe.stringOrNull("externalTabName"),
                amountLabel = formatStoredAmount(1_000_000L, 2, null),
                memo = recipe.stringOrNull("memo").orEmpty(),
                imageContentHash = recipe.stringOrNull("imageAssetId")
                    ?.let { imageHashes[idKey(it)] },
                calories = nutrition.calories.roundToInt(),
                proteinG = nutrition.proteinG,
                fatG = nutrition.fatG,
                carbG = nutrition.carbG,
                items = buildExternalCardItems(
                    recipeId = id,
                    ingredientRows = ingredientRows,
                    recipeRows = recipeRows,
                    foodRows = foodRows,
                    foodPortionRows = foodPortionRows,
                    imageHashes = imageHashes,
                ),
            )
        }
        .sortedWith(
            compareBy<DriveExternalCard> { it.storeName.orEmpty() }
                .thenBy { it.tabName.orEmpty() }
                .thenBy { it.name }
                .thenBy { it.id },
        )
        .toList()

    private fun buildExternalCardItems(
        recipeId: String,
        ingredientRows: Map<String, List<SnapshotRow>>,
        recipeRows: Map<String, SnapshotRow>,
        foodRows: Map<String, SnapshotRow>,
        foodPortionRows: Map<String, List<SnapshotRow>>,
        imageHashes: Map<String, String>,
    ): List<DrivePlanItem> {
        return ingredientRows[idKey(recipeId)]
            .orEmpty()
            .asSequence()
            .mapNotNull { ingredientRow ->
                val ingredient = ingredientRow.payload ?: return@mapNotNull null
                if (!ingredient.optBooleanIgnoreCase("isEnabled", true)) return@mapNotNull null
                ingredientRow
            }
            .sortedWith(
                compareBy<SnapshotRow> { it.payload?.optIntIgnoreCase("displayOrder", 0) ?: 0 }
                    .thenBy { it.rowKey },
            )
            .mapNotNull { ingredientRow ->
                val ingredient = ingredientRow.payload ?: return@mapNotNull null
                val ingredientId = ingredient.stringOrNull("id") ?: ingredientRow.rowKey
                val componentType = ingredient.optIntIgnoreCase("componentType", 0)
                val childId = if (componentType == 1) {
                    ingredient.stringOrNull("referencedRecipeId")
                } else {
                    ingredient.stringOrNull("foodId")
                }
                val child = if (componentType == 1) {
                    recipeRows[idKey(childId)]?.payload
                } else {
                    foodRows[idKey(childId)]?.payload
                }
                val nutrition = nutritionForStoredIngredient(
                    ingredient = ingredient,
                    recipeRows = recipeRows,
                    ingredientRows = ingredientRows,
                    foodRows = foodRows,
                    foodPortionRows = foodPortionRows,
                    visiting = emptySet(),
                )
                DrivePlanItem(
                    name = child?.stringOrNull("name")
                        ?: child?.stringOrNull("foodName")
                        ?: if (componentType == 1) "レシピ" else "食品",
                    amountLabel = formatStoredAmount(
                        amount = ingredient.optLongIgnoreCase("standardAmount", 0L),
                        unit = ingredient.optIntIgnoreCase("standardUnit", -1),
                        customUnitName = ingredient.stringOrNull("standardCustomUnitName"),
                    ),
                    isMainDish = false,
                    isMainDishCandidate = false,
                    imageContentHash = child?.stringOrNull("imageAssetId")
                        ?.let { imageHashes[idKey(it)] },
                    calories = nutrition.calories.roundToInt(),
                    proteinG = nutrition.proteinG,
                    fatG = nutrition.fatG,
                    carbG = nutrition.carbG,
                    id = ingredientId,
                )
            }
            .toList()
    }

    private fun nutritionForStoredRecipe(
        recipe: JSONObject,
        recipeRows: Map<String, SnapshotRow>,
        ingredientRows: Map<String, List<SnapshotRow>>,
        foodRows: Map<String, SnapshotRow>,
        foodPortionRows: Map<String, List<SnapshotRow>>,
        visiting: Set<String>,
    ): NutritionTotals {
        val directNutrition = externalNutrition(recipe)
        if (directNutrition != null) return directNutrition

        val recipeId = idKey(recipe.stringOrNull("id"))
        if (recipeId.isBlank() || recipeId in visiting) return NutritionTotals()
        val nextVisiting = visiting + recipeId
        return ingredientRows[recipeId]
            .orEmpty()
            .asSequence()
            .mapNotNull { ingredientRow ->
                val ingredient = ingredientRow.payload ?: return@mapNotNull null
                if (!ingredient.optBooleanIgnoreCase("isEnabled", true)) return@mapNotNull null
                nutritionForStoredIngredient(
                    ingredient = ingredient,
                    recipeRows = recipeRows,
                    ingredientRows = ingredientRows,
                    foodRows = foodRows,
                    foodPortionRows = foodPortionRows,
                    visiting = nextVisiting,
                )
            }
            .fold(NutritionTotals()) { total, ingredient -> total + ingredient }
    }

    private fun nutritionForStoredIngredient(
        ingredient: JSONObject,
        recipeRows: Map<String, SnapshotRow>,
        ingredientRows: Map<String, List<SnapshotRow>>,
        foodRows: Map<String, SnapshotRow>,
        foodPortionRows: Map<String, List<SnapshotRow>>,
        visiting: Set<String>,
    ): NutritionTotals {
        val componentType = ingredient.optIntIgnoreCase("componentType", 0)
        if (componentType == 1) {
            val referencedRecipeId = ingredient.stringOrNull("referencedRecipeId")
                ?: return NutritionTotals()
            val referencedRecipe = recipeRows[idKey(referencedRecipeId)]?.payload
                ?: return NutritionTotals()
            return nutritionForStoredRecipe(
                recipe = referencedRecipe,
                recipeRows = recipeRows,
                ingredientRows = ingredientRows,
                foodRows = foodRows,
                foodPortionRows = foodPortionRows,
                visiting = visiting,
            ).scaled(recipeQuantityFactor(ingredient, referencedRecipe))
        }

        val foodId = ingredient.stringOrNull("foodId") ?: return NutritionTotals()
        return nutritionForFood(
            food = foodRows[idKey(foodId)]?.payload,
            item = ingredient,
            portionRows = foodPortionRows[idKey(foodId)].orEmpty(),
        )
    }

    private fun externalNutrition(recipe: JSONObject): NutritionTotals? {
        val energy = recipe.longOrNullIgnoreCase("externalEnergyTenthsKcal")
        val protein = recipe.longOrNullIgnoreCase("externalProteinMilligrams")
        val fat = recipe.longOrNullIgnoreCase("externalFatMilligrams")
        val carbohydrate = recipe.longOrNullIgnoreCase("externalCarbohydrateMilligrams")
        if (energy == null || protein == null || fat == null || carbohydrate == null) return null
        return NutritionTotals(
            calories = energy / 10.0,
            proteinG = protein / 1_000.0,
            fatG = fat / 1_000.0,
            carbG = carbohydrate / 1_000.0,
        )
    }

    private fun buildMeal(
        slot: Int,
        mealRow: SnapshotRow?,
        itemRows: List<SnapshotRow>,
        foodRows: Map<String, SnapshotRow>,
        recipeRows: Map<String, SnapshotRow>,
        foodPortionRows: Map<String, List<SnapshotRow>>,
        recipeIngredientRows: Map<String, List<SnapshotRow>>,
        recipePortionRows: Map<String, List<SnapshotRow>>,
        imageHashes: Map<String, String>,
    ): DrivePlanMeal {
        val meal = mealRow?.payload
        val mealId = meal?.stringOrNull("id") ?: mealRow?.rowKey
        val selectedMainDishItemId = meal?.stringOrNull("selectedMainDishItemId")
        val builtItems = itemRows
            .asSequence()
            .filter { itemRow ->
                val item = itemRow.payload ?: return@filter false
                item.optBooleanIgnoreCase("isEnabled", true) &&
                    idKey(item.stringOrNull("dailyPlanMealSnapshotId")) == idKey(mealId)
            }
            .sortedWith(compareBy<SnapshotRow> { it.payload?.optIntIgnoreCase("displayOrder", 0) ?: 0 }.thenBy { it.rowKey })
            .mapNotNull { itemRow ->
                val item = itemRow.payload ?: return@mapNotNull null
                val itemId = item.stringOrNull("id") ?: itemRow.rowKey
                val componentType = item.optIntIgnoreCase("componentType", 0)
                val child = if (componentType == 1) {
                    recipeRows[idKey(itemId)]?.payload
                } else {
                    foodRows[idKey(itemId)]?.payload
                }
                val nutrition = if (componentType == 1) {
                    nutritionForRecipe(
                        recipe = child,
                        item = item,
                        ingredients = recipeIngredientRows[idKey(itemId)].orEmpty(),
                        recipePortionRows = recipePortionRows,
                    )
                } else {
                    nutritionForFood(
                        food = child,
                        item = item,
                        portionRows = foodPortionRows[idKey(itemId)].orEmpty(),
                    )
                }
                val imageAssetId = child?.stringOrNull("imageAssetId")
                val isMainDish = idKey(itemId) == idKey(selectedMainDishItemId)
                val isMainDishCandidate = item.optBooleanIgnoreCase("isMainDishCandidate", false) || isMainDish
                val driveItem = DrivePlanItem(
                    name = child?.stringOrNull("name")
                        ?: child?.stringOrNull("foodName")
                        ?: if (componentType == 1) "レシピ" else "食品",
                    amountLabel = formatStoredAmount(
                        amount = item.optLongIgnoreCase("standardAmount", 0L),
                        unit = item.optIntIgnoreCase("standardUnit", -1),
                        customUnitName = item.stringOrNull("standardCustomUnitName"),
                    ),
                    isMainDish = isMainDish,
                    isMainDishCandidate = isMainDishCandidate,
                    imageContentHash = imageAssetId?.let { imageHashes[idKey(it)] },
                    calories = nutrition.calories.roundToInt(),
                    proteinG = nutrition.proteinG,
                    fatG = nutrition.fatG,
                    carbG = nutrition.carbG,
                    id = itemId,
                )
                BuiltItem(
                    item = driveItem,
                    nutrition = if (driveItem.isIncludedInMealNutrition()) nutrition else NutritionTotals(),
                )
            }
            .toList()
        val items = builtItems.map(BuiltItem::item)
        val nutrition = builtItems.fold(NutritionTotals()) { total, built -> total + built.nutrition }

        return DrivePlanMeal(
            slot = slot,
            label = drivePlanMealLabel(slot),
            name = meal?.stringOrNull("name").orEmpty().ifBlank { "未設定" },
            memo = meal?.stringOrNull("memo").orEmpty(),
            imageContentHash = meal?.stringOrNull("imageAssetId")?.let { imageHashes[idKey(it)] },
            items = items,
            totalCalories = nutrition.calories.roundToInt(),
            proteinG = nutrition.proteinG,
            fatG = nutrition.fatG,
            carbG = nutrition.carbG,
            nutritionDataAvailable = true,
        )
    }

    private fun nutritionForFood(
        food: JSONObject?,
        item: JSONObject,
        portionRows: List<SnapshotRow>,
    ): NutritionTotals {
        if (food == null) return NutritionTotals()
        val factor = quantityFactor(
            item = item,
            reference = food,
            portionRows = portionRows,
        )
        return NutritionTotals(
            calories = food.optLongIgnoreCase("energyTenthsKcal", 0L) / 10.0 * factor,
            proteinG = food.optLongIgnoreCase("proteinMilligrams", 0L) / 1_000.0 * factor,
            fatG = food.optLongIgnoreCase("fatMilligrams", 0L) / 1_000.0 * factor,
            carbG = food.optLongIgnoreCase("carbohydrateMilligrams", 0L) / 1_000.0 * factor,
        )
    }

    private fun nutritionForRecipe(
        recipe: JSONObject?,
        item: JSONObject,
        ingredients: List<SnapshotRow>,
        recipePortionRows: Map<String, List<SnapshotRow>>,
    ): NutritionTotals {
        if (recipe == null) return NutritionTotals()

        val externalEnergy = recipe.longOrNullIgnoreCase("externalEnergyTenthsKcal")
        val externalProtein = recipe.longOrNullIgnoreCase("externalProteinMilligrams")
        val externalFat = recipe.longOrNullIgnoreCase("externalFatMilligrams")
        val externalCarbohydrate = recipe.longOrNullIgnoreCase("externalCarbohydrateMilligrams")
        val factor = recipeQuantityFactor(item, recipe)
        if (externalEnergy != null &&
            externalProtein != null &&
            externalFat != null &&
            externalCarbohydrate != null
        ) {
            return NutritionTotals(
                calories = externalEnergy / 10.0 * factor,
                proteinG = externalProtein / 1_000.0 * factor,
                fatG = externalFat / 1_000.0 * factor,
                carbG = externalCarbohydrate / 1_000.0 * factor,
            )
        }

        val wholeNutrition = ingredients
            .asSequence()
            .mapNotNull { ingredientRow ->
                val ingredient = ingredientRow.payload ?: return@mapNotNull null
                if (!ingredient.optBooleanIgnoreCase("isEnabled", true)) return@mapNotNull null
                val ingredientId = ingredient.stringOrNull("id") ?: ingredientRow.rowKey
                nutritionForFood(
                    food = ingredient,
                    item = ingredient,
                    portionRows = recipePortionRows[idKey(ingredientId)].orEmpty(),
                )
            }
            .fold(NutritionTotals()) { total, ingredient -> total + ingredient }
        return wholeNutrition.scaled(factor)
    }

    private fun quantityFactor(
        item: JSONObject,
        reference: JSONObject,
        portionRows: List<SnapshotRow>,
    ): Double {
        val amount = item.optLongIgnoreCase("standardAmount", 0L)
        val referenceAmount = reference.optLongIgnoreCase("referenceAmount", 0L)
        if (amount <= 0L || referenceAmount <= 0L) return 0.0

        val itemUnit = item.optIntIgnoreCase("standardUnit", -1)
        val referenceUnit = reference.optIntIgnoreCase("referenceUnit", -1)
        val itemCustomUnitName = item.stringOrNull("standardCustomUnitName")
        val referenceCustomUnitName = reference.stringOrNull("referenceCustomUnitName")
        if (sameUnit(itemUnit, itemCustomUnitName, referenceUnit, referenceCustomUnitName)) {
            return amount.toDouble() / referenceAmount
        }

        val payloads = portionRows.mapNotNull(SnapshotRow::payload)
        val portion = payloads.firstOrNull { portion ->
            portion.optIntIgnoreCase("sourceUnit", -1) == itemUnit &&
                customUnitMatches(
                    itemUnit,
                    itemCustomUnitName,
                    portion.stringOrNull("sourceCustomUnitName"),
                )
        } ?: payloads.firstOrNull { portion ->
            portion.optIntIgnoreCase("sourceUnit", -1) == itemUnit
        } ?: return 0.0

        val sourceAmount = portion.optLongIgnoreCase("sourceAmount", 0L)
        val equivalentAmount = portion.optLongIgnoreCase("standardEquivalentAmount", 0L)
        val equivalentUnit = portion.optIntIgnoreCase("standardEquivalentUnit", -1)
        if (sourceAmount <= 0L || equivalentAmount <= 0L) return 0.0
        if (!sameUnit(
                equivalentUnit,
                portion.stringOrNull("standardEquivalentCustomUnitName"),
                referenceUnit,
                referenceCustomUnitName,
            )
        ) {
            return 0.0
        }
        return amount.toDouble() / sourceAmount * equivalentAmount / referenceAmount
    }

    private fun recipeQuantityFactor(item: JSONObject, recipe: JSONObject): Double {
        val amount = item.optLongIgnoreCase("standardAmount", 0L)
        val unit = item.optIntIgnoreCase("standardUnit", -1)
        val finishedWeightMg = recipe.optLongIgnoreCase("finishedWeightMg", 0L)
        if (amount <= 0L || finishedWeightMg <= 0L) return 0.0

        return when (unit) {
            0 -> amount.toDouble() / finishedWeightMg
            2 -> {
                val servingWeightMg = recipe.optLongIgnoreCase("standardServingWeightMg", 0L)
                if (servingWeightMg <= 0L) 0.0
                else amount.toDouble() / 1_000_000.0 * servingWeightMg / finishedWeightMg
            }
            else -> 0.0
        }
    }

    private fun sameUnit(
        leftUnit: Int,
        leftCustomUnitName: String?,
        rightUnit: Int,
        rightCustomUnitName: String?,
    ): Boolean = leftUnit == rightUnit && customUnitMatches(
        leftUnit,
        leftCustomUnitName,
        rightCustomUnitName,
    )

    private fun customUnitMatches(unit: Int, left: String?, right: String?): Boolean {
        if (unit != 8) return true
        if (left.isNullOrBlank() || right.isNullOrBlank()) return true
        return left.equals(right, ignoreCase = true)
    }

    private fun groupRowsByField(
        rows: Collection<SnapshotRow>,
        field: String,
    ): Map<String, List<SnapshotRow>> = rows
        .mapNotNull { row ->
            val id = row.payload?.stringOrNull(field) ?: return@mapNotNull null
            idKey(id) to row
        }
        .groupBy({ it.first }, { it.second })

    private fun JSONObject.isEatingOutRecipe(): Boolean = when (val kind = valueIgnoreCase("kind")) {
        is Number -> kind.toInt() == 1
        is String -> when (kind.lowercase(Locale.ROOT)) {
            "1", "eatingout", "eating_out", "eating-out" -> true
            else -> false
        }
        else -> false
    }

    private fun formatStoredAmount(amount: Long, unit: Int, customUnitName: String?): String {
        if (unit < 0) return ""
        val scale = if (unit == 0 || unit == 1) 1_000L else 1_000_000L
        val value = BigDecimal.valueOf(amount)
            .divide(BigDecimal.valueOf(scale))
            .stripTrailingZeros()
            .toPlainString()
        val unitName = when (unit) {
            0 -> "g"
            1 -> "ml"
            2 -> "個"
            3 -> "パック"
            4 -> "切れ"
            5 -> "カップ"
            6 -> "本"
            7 -> "枚"
            8 -> customUnitName.orEmpty()
            else -> ""
        }
        return if (unitName.isBlank()) value else "$value $unitName"
    }

    private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun SnapshotRow.isEntity(simpleName: String): Boolean =
        entityType == simpleName || entityType.endsWith(".$simpleName")

    private fun idKey(value: String?): String = value.orEmpty().lowercase(Locale.ROOT)

    private data class BuiltItem(
        val item: DrivePlanItem,
        val nutrition: NutritionTotals,
    )

    private data class NutritionTotals(
        val calories: Double = 0.0,
        val proteinG: Double = 0.0,
        val fatG: Double = 0.0,
        val carbG: Double = 0.0,
    ) {
        operator fun plus(other: NutritionTotals): NutritionTotals = NutritionTotals(
            calories = calories + other.calories,
            proteinG = proteinG + other.proteinG,
            fatG = fatG + other.fatG,
            carbG = carbG + other.carbG,
        )

        fun scaled(factor: Double): NutritionTotals = NutritionTotals(
            calories = calories * factor,
            proteinG = proteinG * factor,
            fatG = fatG * factor,
            carbG = carbG * factor,
        )
    }
}
