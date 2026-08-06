package com.gonec009.meshizandaka.data.drive

import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Locale
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

        val mealRows = rows.filter { it.isEntity("DailyPlanMealSnapshotEntity") }
        val itemRows = rows.filter { it.isEntity("DailyPlanMealItemSnapshotEntity") }
        val foodRows = rows.filter { it.isEntity("DailyPlanFoodSnapshotEntity") }
            .associateBy { idKey(it.rowKey) }
        val recipeRows = rows.filter { it.isEntity("DailyPlanRecipeSnapshotEntity") }
            .associateBy { idKey(it.rowKey) }

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

        return DrivePlanCatalog(
            plans = plans,
            preferredPlanId = preferredPlanId,
        )
    }

    private fun buildMeal(
        slot: Int,
        mealRow: SnapshotRow?,
        itemRows: List<SnapshotRow>,
        foodRows: Map<String, SnapshotRow>,
        recipeRows: Map<String, SnapshotRow>,
        imageHashes: Map<String, String>,
    ): DrivePlanMeal {
        val meal = mealRow?.payload
        val mealId = meal?.stringOrNull("id") ?: mealRow?.rowKey
        val selectedMainDishItemId = meal?.stringOrNull("selectedMainDishItemId")
        val items = itemRows
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
                val imageAssetId = child?.stringOrNull("imageAssetId")
                DrivePlanItem(
                    name = child?.stringOrNull("name")
                        ?: child?.stringOrNull("foodName")
                        ?: if (componentType == 1) "レシピ" else "食品",
                    amountLabel = formatStoredAmount(
                        amount = item.optLongIgnoreCase("standardAmount", 0L),
                        unit = item.optIntIgnoreCase("standardUnit", -1),
                        customUnitName = item.stringOrNull("standardCustomUnitName"),
                    ),
                    isMainDish = idKey(itemId) == idKey(selectedMainDishItemId),
                    imageContentHash = imageAssetId?.let { imageHashes[idKey(it)] },
                )
            }
            .toList()

        return DrivePlanMeal(
            slot = slot,
            label = drivePlanMealLabel(slot),
            name = meal?.stringOrNull("name").orEmpty().ifBlank { "未設定" },
            memo = meal?.stringOrNull("memo").orEmpty(),
            imageContentHash = meal?.stringOrNull("imageAssetId")?.let { imageHashes[idKey(it)] },
            items = items,
        )
    }

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
