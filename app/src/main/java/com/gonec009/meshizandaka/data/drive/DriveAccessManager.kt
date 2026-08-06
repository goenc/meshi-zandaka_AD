package com.gonec009.meshizandaka.data.drive

import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DriveConnectionPhase {
    IDLE,
    CONNECTING,
    CONNECTED,
    PARTIAL,
    FAILED,
}

data class DriveConnectionState(
    val phase: DriveConnectionPhase = DriveConnectionPhase.IDLE,
    val syncBatchCount: Int = 0,
    val imageCount: Int = 0,
    val backupFolderFound: Boolean = false,
    val backupFileCount: Int = 0,
    val appDataErrorCode: Int? = null,
    val backupErrorCode: Int? = null,
)

class DriveAccessManager(
    private val client: GoogleDriveClient,
) {
    private val _state = MutableStateFlow(DriveConnectionState())
    private var accessToken: String? = null

    val state: StateFlow<DriveConnectionState> = _state.asStateFlow()

    fun markAuthorizationStarted() {
        _state.update { it.copy(phase = DriveConnectionPhase.CONNECTING) }
    }

    fun markAuthorizationFailed() {
        accessToken = null
        _state.value = DriveConnectionState(phase = DriveConnectionPhase.FAILED)
    }

    suspend fun connect(token: String) {
        require(token.isNotBlank()) { "Googleアクセストークンが空です。" }
        _state.value = DriveConnectionState(phase = DriveConnectionPhase.CONNECTING)
        accessToken = token

        val appDataResult = capture { client.inspectAppData(token) }
        val backupResult = capture { client.inspectBackupFolder(token) }
        val appData = appDataResult.getOrNull()
        val backup = backupResult.getOrNull()
        val phase = when {
            appDataResult.isSuccess && backupResult.isSuccess -> DriveConnectionPhase.CONNECTED
            appDataResult.isSuccess || backupResult.isSuccess -> DriveConnectionPhase.PARTIAL
            else -> DriveConnectionPhase.FAILED
        }
        _state.value = DriveConnectionState(
            phase = phase,
            syncBatchCount = appData?.syncBatchCount ?: 0,
            imageCount = appData?.imageCount ?: 0,
            backupFolderFound = backup?.folderFound == true,
            backupFileCount = backup?.backupFileCount ?: 0,
            appDataErrorCode = appDataResult.exceptionOrNull()?.let { (it as? DriveApiException)?.statusCode },
            backupErrorCode = backupResult.exceptionOrNull()?.let { (it as? DriveApiException)?.statusCode },
        )
    }

    suspend fun downloadFile(fileId: String): ByteArray {
        val token = accessToken ?: error("Google Driveへ接続していません。")
        return client.downloadFile(token, fileId)
    }

    private suspend fun <T> capture(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    companion object {
        val authorizationScopes: List<Scope> = listOf(
            Scope(GoogleDriveClient.APP_DATA_SCOPE),
            Scope(GoogleDriveClient.BACKUP_SCOPE),
        )
    }
}
