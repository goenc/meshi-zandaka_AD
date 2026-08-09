package com.gonec009.meshizandaka.data.drive

import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Windows版がappDataFolderへ書き出した同期ログを、Android表示用の計画へ復元する読込層。
 * Android側では同期ログを変更せず、最新Revisionの行だけを採用する。
 */
class DrivePlanSnapshotReader(
    private val client: GoogleDriveClient,
) {
    suspend fun load(accessToken: String): DrivePlanCatalog = withContext(Dispatchers.IO) {
        val rows = linkedMapOf<String, SnapshotRow>()
        val batches = client.listSyncBatches(accessToken)
            .sortedWith(
                compareBy<DriveSyncBatchMetadata> { it.deviceId }
                    .thenBy { it.deviceSequence }
                    .thenBy { it.name }
                    .thenBy { it.id },
            )

        batches.forEach { descriptor ->
            val content = client.downloadFile(accessToken, descriptor.id)
                .toString(StandardCharsets.UTF_8)
            applyBatch(JSONObject(content), rows)
        }
        buildCatalog(rows.values)
    }

    private fun applyBatch(
        batch: JSONObject,
        rows: MutableMap<String, SnapshotRow>,
    ) {
        val datasetId = batch.stringOrNull("datasetId")
        if (!datasetId.isNullOrBlank() && !datasetId.equals(GoogleDriveClient.DATASET_ID, ignoreCase = true)) return
        val changes = batch.optJSONArrayIgnoreCase("changes") ?: JSONArray()
        for (index in 0 until changes.length()) {
            val change = changes.optJSONObject(index) ?: continue
            val entityType = change.stringOrNull("entityType") ?: continue
            val rowKey = change.stringOrNull("rowKey") ?: continue
            val revision = parseRevision(change.optJSONObjectIgnoreCase("revision")) ?: continue
            val key = "$entityType\u0000$rowKey"
            val current = rows[key]
            if (current != null && revision <= current.revision) continue

            when (changeKind(change)) {
                0 -> {
                    val payload = parsePayload(change.valueIgnoreCase("payloadJson")) ?: continue
                    rows[key] = SnapshotRow(
                        entityType = entityType,
                        rowKey = rowKey,
                        payload = payload,
                        revision = revision,
                    )
                }
                1 -> {
                    rows[key] = SnapshotRow(
                        entityType = entityType,
                        rowKey = rowKey,
                        payload = null,
                        revision = revision,
                    )
                }
            }
        }
    }

    private fun buildCatalog(rows: Collection<SnapshotRow>): DrivePlanCatalog {
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

        return DrivePlanCatalog(
            plans = plans,
            preferredPlanId = preferredPlanId,
            externalCards = externalCards,
        )
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

    private fun parsePayload(value: Any?): JSONObject? {
        if (value == null || value == JSONObject.NULL) return null
        return when (value) {
            is JSONObject -> value
            is String -> value.takeIf { it.isNotBlank() }?.let(::JSONObject)
            else -> JSONObject(value.toString())
        }
    }

    private fun parseRevision(value: JSONObject?): SnapshotRevision? {
        if (value == null) return null
        val operationId = value.stringOrNull("operationId") ?: return null
        return SnapshotRevision(
            editedUtcTicks = value.optLongIgnoreCase("editedUtcTicks", Long.MIN_VALUE),
            deviceId = value.stringOrNull("deviceId").orEmpty(),
            deviceSequence = value.optLongIgnoreCase("deviceSequence", Long.MIN_VALUE),
            operationId = operationId,
        )
    }

    private fun changeKind(change: JSONObject): Int {
        val value = change.valueIgnoreCase("changeKind")
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: when (value.lowercase(Locale.ROOT)) {
                "delete" -> 1
                "upsert" -> 0
                else -> -1
            }
            else -> -1
        }
    }

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

    private fun SnapshotRow.isEntity(simpleName: String): Boolean =
        entityType == simpleName || entityType.endsWith(".$simpleName")

    private fun idKey(value: String?): String = value.orEmpty().lowercase(Locale.ROOT)

    private data class SnapshotRow(
        val entityType: String,
        val rowKey: String,
        val payload: JSONObject?,
        val revision: SnapshotRevision,
    )

    private data class SnapshotRevision(
        val editedUtcTicks: Long,
        val deviceId: String,
        val deviceSequence: Long,
        val operationId: String,
    ) : Comparable<SnapshotRevision> {
        override fun compareTo(other: SnapshotRevision): Int =
            compareValuesBy(
                this,
                other,
                SnapshotRevision::editedUtcTicks,
                SnapshotRevision::deviceId,
                SnapshotRevision::deviceSequence,
                SnapshotRevision::operationId,
            )
    }

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

/**
 * Windows側の同期ログはエンベロープと行PayloadでJSONの大文字小文字が異なるため、
 * Android側ではキー名を大小文字非依存で読む。camelCaseもそのまま利用できる。
 */
internal fun JSONObject.valueIgnoreCase(key: String): Any? {
    val iterator = keys()
    while (iterator.hasNext()) {
        val actualKey = iterator.next()
        if (actualKey.equals(key, ignoreCase = true)) {
            return opt(actualKey).takeUnless { it == JSONObject.NULL }
        }
    }
    return null
}

internal fun JSONObject.stringOrNull(key: String): String? =
    valueIgnoreCase(key)
        ?.toString()
        ?.takeIf { it.isNotBlank() }

internal fun JSONObject.optJSONArrayIgnoreCase(key: String): JSONArray? =
    valueIgnoreCase(key) as? JSONArray

internal fun JSONObject.optJSONObjectIgnoreCase(key: String): JSONObject? =
    valueIgnoreCase(key) as? JSONObject

internal fun JSONObject.optBooleanIgnoreCase(key: String, default: Boolean): Boolean =
    when (val value = valueIgnoreCase(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> when (value.lowercase(Locale.ROOT)) {
            "true", "1" -> true
            "false", "0" -> false
            else -> default
        }
        else -> default
    }

internal fun JSONObject.optIntIgnoreCase(key: String, default: Int): Int =
    when (val value = valueIgnoreCase(key)) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: default
        else -> default
    }

internal fun JSONObject.optLongIgnoreCase(key: String, default: Long): Long =
    when (val value = valueIgnoreCase(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: default
        else -> default
    }

internal fun JSONObject.longOrNullIgnoreCase(key: String): Long? =
    when (val value = valueIgnoreCase(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
