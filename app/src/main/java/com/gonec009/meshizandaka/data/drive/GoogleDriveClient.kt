package com.gonec009.meshizandaka.data.drive

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String?,
    val appProperties: Map<String, String>,
)

data class DriveAppDataSummary(
    val syncBatchCount: Int,
    val imageCount: Int,
)

data class DriveBackupSummary(
    val folderFound: Boolean,
    val backupFileCount: Int,
)

data class DriveSyncBatchMetadata(
    val id: String,
    val name: String,
    val deviceId: String,
    val deviceSequence: Long,
)

data class DriveImageMetadata(
    val id: String,
    val name: String,
    val contentHash: String,
    val mimeType: String?,
)

class DriveApiException(
    val statusCode: Int,
    message: String,
) : IOException(message)

class GoogleDriveClient {
    suspend fun listSyncBatches(accessToken: String): List<DriveSyncBatchMetadata> = withContext(Dispatchers.IO) {
        listFiles(
            accessToken = accessToken,
            spaces = APP_DATA_SPACE,
            query = "'appDataFolder' in parents and trashed = false and name contains '$SYNC_FILE_PREFIX'",
        ).mapNotNull { file ->
            if (file.appProperties[DATASET_PROPERTY] != DATASET_ID) return@mapNotNull null
            val deviceId = file.appProperties[DEVICE_ID_PROPERTY].orEmpty()
            val deviceSequence = file.appProperties[DEVICE_SEQUENCE_PROPERTY]?.toLongOrNull() ?: return@mapNotNull null
            if (deviceId.isBlank()) return@mapNotNull null
            DriveSyncBatchMetadata(
                id = file.id,
                name = file.name,
                deviceId = deviceId,
                deviceSequence = deviceSequence,
            )
        }
    }

    suspend fun listImages(accessToken: String): List<DriveImageMetadata> = withContext(Dispatchers.IO) {
        listFiles(
            accessToken = accessToken,
            spaces = APP_DATA_SPACE,
            query = "'appDataFolder' in parents and trashed = false and appProperties has { key='$IMAGE_KIND_PROPERTY' and value='$IMAGE_KIND' }",
        ).mapNotNull { file ->
            if (file.appProperties[DATASET_PROPERTY] != DATASET_ID) return@mapNotNull null
            val contentHash = file.appProperties[CONTENT_HASH_PROPERTY].orEmpty()
            if (contentHash.isBlank()) return@mapNotNull null
            DriveImageMetadata(
                id = file.id,
                name = file.name,
                contentHash = contentHash,
                mimeType = file.appProperties[MIME_TYPE_PROPERTY] ?: file.mimeType,
            )
        }
    }

    suspend fun inspectAppData(accessToken: String): DriveAppDataSummary = withContext(Dispatchers.IO) {
        val files = listFiles(
            accessToken = accessToken,
            spaces = APP_DATA_SPACE,
            query = "'appDataFolder' in parents and trashed = false",
        )
        val targetFiles = files.filter { file ->
            file.appProperties[DATASET_PROPERTY] == DATASET_ID
        }
        DriveAppDataSummary(
            syncBatchCount = targetFiles.count { file -> file.name.startsWith(SYNC_FILE_PREFIX) },
            imageCount = targetFiles.count { file -> file.appProperties[IMAGE_KIND_PROPERTY] == IMAGE_KIND },
        )
    }

    suspend fun inspectBackupFolder(accessToken: String): DriveBackupSummary = withContext(Dispatchers.IO) {
        val rootFolder = findFolder(
            accessToken = accessToken,
            name = BACKUP_ROOT_FOLDER_NAME,
            kind = BACKUP_ROOT_FOLDER_KIND,
            parentId = null,
        ) ?: return@withContext DriveBackupSummary(folderFound = false, backupFileCount = 0)
        val backupFolder = findFolder(
            accessToken = accessToken,
            name = BACKUP_FOLDER_NAME,
            kind = BACKUP_FOLDER_KIND,
            parentId = rootFolder.id,
        ) ?: return@withContext DriveBackupSummary(folderFound = false, backupFileCount = 0)
        val backups = listFiles(
            accessToken = accessToken,
            spaces = DRIVE_SPACE,
            query = "'${escapeQueryLiteral(backupFolder.id)}' in parents and trashed = false and mimeType = '$ZIP_MIME_TYPE'",
        )
        DriveBackupSummary(folderFound = true, backupFileCount = backups.size)
    }

    suspend fun downloadFile(accessToken: String, fileId: String): ByteArray = withContext(Dispatchers.IO) {
        require(fileId.isNotBlank()) { "DriveファイルIDが空です。" }
        val url = "$DRIVE_API_BASE/files/${Uri.encode(fileId)}".toUri()
            .buildUpon()
            .appendQueryParameter("alt", "media")
            .build()
            .toString()
        executeBytes(accessToken, url)
    }

    suspend fun readEstimatedCalorieSummary(
        accessToken: String,
        today: LocalDate = LocalDate.now(),
    ): DriveCalorieSummary = withContext(Dispatchers.IO) {
        val url = "$SHEETS_API_BASE/spreadsheets/${Uri.encode(BODY_DATA_SPREADSHEET_ID)}/values/" +
            Uri.encode(BODY_DATA_RANGE)
        val requestUrl = url.toUri().buildUpon()
            .appendQueryParameter("majorDimension", "ROWS")
            .appendQueryParameter("valueRenderOption", "UNFORMATTED_VALUE")
            .build()
            .toString()
        val root = JSONObject(executeText(accessToken, requestUrl, "Google Sheets API"))
        val values = root.optJSONArray("values") ?: JSONArray()
        val rows = (0 until values.length()).map { rowIndex ->
            val row = values.optJSONArray(rowIndex) ?: JSONArray()
            (0 until row.length()).map { columnIndex ->
                row.opt(columnIndex).takeUnless { it == JSONObject.NULL }
            }
        }
        buildDriveCalorieSummary(rows, today)
    }

    private fun findFolder(
        accessToken: String,
        name: String,
        kind: String,
        parentId: String?,
    ): DriveFileMetadata? {
        val conditions = mutableListOf(
            "name = '${escapeQueryLiteral(name)}'",
            "mimeType = '$FOLDER_MIME_TYPE'",
            "trashed = false",
            "appProperties has { key='$FOLDER_KIND_PROPERTY' and value='${escapeQueryLiteral(kind)}' }",
        )
        if (!parentId.isNullOrBlank()) {
            conditions += "'${escapeQueryLiteral(parentId)}' in parents"
        }
        return listFiles(
            accessToken = accessToken,
            spaces = DRIVE_SPACE,
            query = conditions.joinToString(" and "),
        ).firstOrNull()
    }

    private fun listFiles(
        accessToken: String,
        spaces: String?,
        query: String,
    ): List<DriveFileMetadata> {
        require(accessToken.isNotBlank()) { "Googleアクセストークンが空です。" }
        val result = mutableListOf<DriveFileMetadata>()
        var pageToken: String? = null
        do {
            val builder = "$DRIVE_API_BASE/files".toUri()
                .buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("pageSize", PAGE_SIZE.toString())
                .appendQueryParameter("fields", FILE_FIELDS)
            if (!spaces.isNullOrBlank()) {
                builder.appendQueryParameter("spaces", spaces)
            }
            if (!pageToken.isNullOrBlank()) {
                builder.appendQueryParameter("pageToken", pageToken)
            }
            val root = JSONObject(executeText(accessToken, builder.build().toString()))
            val files = root.optJSONArray("files") ?: JSONArray()
            for (index in 0 until files.length()) {
                result += parseFile(files.getJSONObject(index))
            }
            pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (!pageToken.isNullOrBlank())
        return result
    }

    private fun parseFile(json: JSONObject): DriveFileMetadata {
        val properties = mutableMapOf<String, String>()
        val appProperties = json.optJSONObject("appProperties")
        if (appProperties != null) {
            val keys = appProperties.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                properties[key] = appProperties.optString(key)
            }
        }
        return DriveFileMetadata(
            id = json.optString("id"),
            name = json.optString("name"),
            mimeType = json.optString("mimeType").takeIf { it.isNotBlank() },
            appProperties = properties,
        )
    }

    private fun executeText(
        accessToken: String,
        url: String,
        apiName: String = "Google Drive API",
    ): String {
        val connection = openConnection(accessToken, url)
        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) {
                throw DriveApiException(responseCode, "${apiName}への接続に失敗しました。")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun executeBytes(accessToken: String, url: String): ByteArray {
        val connection = openConnection(accessToken, url)
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) {
                connection.errorStream?.close()
                throw DriveApiException(responseCode, "Google Driveファイルの取得に失敗しました。")
            }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(accessToken: String, url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun escapeQueryLiteral(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'")

    companion object {
        const val DATASET_ID = "9b0db1ba-7ee0-4cf3-9b9b-84f674a8d1bb"
        const val APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val BACKUP_SCOPE = "https://www.googleapis.com/auth/drive.readonly"

        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val SHEETS_API_BASE = "https://sheets.googleapis.com/v4"
        private const val BODY_DATA_SPREADSHEET_ID = "1NOmnGrc_bV6ieNpcQNDgh4TYCSxf7gIBI-eB9kjs3HY"
        private const val BODY_DATA_RANGE = "stepDailyRecords!A1:F2000"
        private const val DRIVE_SPACE = "drive"
        private const val APP_DATA_SPACE = "appDataFolder"
        private const val PAGE_SIZE = 1000
        private const val FILE_FIELDS = "nextPageToken,files(id,name,mimeType,appProperties)"
        private const val SYNC_FILE_PREFIX = "pfc-sync-"
        private const val DEVICE_ID_PROPERTY = "deviceId"
        private const val DEVICE_SEQUENCE_PROPERTY = "deviceSequence"
        private const val IMAGE_KIND_PROPERTY = "kind"
        private const val IMAGE_KIND = "image"
        private const val DATASET_PROPERTY = "datasetId"
        private const val CONTENT_HASH_PROPERTY = "contentHash"
        private const val MIME_TYPE_PROPERTY = "mimeType"
        private const val FOLDER_KIND_PROPERTY = "pfcPlanBoardFolderKind"
        private const val BACKUP_ROOT_FOLDER_KIND = "backupRoot"
        private const val BACKUP_FOLDER_KIND = "backup"
        private const val BACKUP_ROOT_FOLDER_NAME = "食事管理アプリ"
        private const val BACKUP_FOLDER_NAME = "バックアップ"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val ZIP_MIME_TYPE = "application/zip"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val HTTP_SUCCESS_MIN = 200
        private const val HTTP_SUCCESS_MAX = 299
    }
}
