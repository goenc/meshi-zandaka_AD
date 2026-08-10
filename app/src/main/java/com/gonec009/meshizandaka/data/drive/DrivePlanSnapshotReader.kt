package com.gonec009.meshizandaka.data.drive

import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val MAX_CONCURRENT_BATCH_DOWNLOADS = 4

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

        val contents = coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_BATCH_DOWNLOADS)
            batches.map { descriptor ->
                async {
                    semaphore.withPermit {
                        client.downloadFile(accessToken, descriptor.id)
                            .toString(StandardCharsets.UTF_8)
                    }
                }
            }.awaitAll()
        }
        contents.forEach { content ->
            applyBatch(JSONObject(content), rows)
        }
        DrivePlanCatalogBuilder.build(rows.values)
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
}

internal data class SnapshotRow(
    val entityType: String,
    val rowKey: String,
    val payload: JSONObject?,
    val revision: SnapshotRevision,
)

internal data class SnapshotRevision(
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
