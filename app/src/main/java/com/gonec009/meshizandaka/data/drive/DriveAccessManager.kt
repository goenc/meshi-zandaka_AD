package com.gonec009.meshizandaka.data.drive

import android.content.Context
import com.google.android.gms.common.api.Scope
import com.gonec009.meshizandaka.data.repository.DrivePlanCacheRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    context: Context,
    private val cacheRepository: DrivePlanCacheRepository,
    private val shortcutSynchronizer: DrivePlanShortcutSynchronizer,
) {
    private val planReader = DrivePlanSnapshotReader(client)
    private val imageCache = DriveImageCache(context)
    private val calorieSummaryCache = DriveCalorieSummaryCache(context)
    private val _state = MutableStateFlow(DriveConnectionState())
    private val _planState = MutableStateFlow(DrivePlanState())
    private val _calorieSummary = MutableStateFlow(calorieSummaryCache.load())
    private var accessToken: String? = null
    private var imageMetadata: Map<String, DriveImageMetadata> = emptyMap()
    private var imageMetadataLoaded = false

    val state: StateFlow<DriveConnectionState> = _state.asStateFlow()
    val planState: StateFlow<DrivePlanState> = _planState.asStateFlow()
    val calorieSummary: StateFlow<DriveCalorieSummary?> = _calorieSummary.asStateFlow()

    fun markAuthorizationStarted() {
        _state.update { it.copy(phase = DriveConnectionPhase.CONNECTING) }
        val current = _planState.value
        _planState.value = if (current.plans.isEmpty()) {
            DrivePlanState(
                phase = DrivePlanPhase.LOADING,
                externalCards = current.externalCards,
                foods = current.foods,
            )
        } else {
            current.copy(errorMessage = null)
        }
    }

    fun markAuthorizationFailed() {
        accessToken = null
        imageMetadata = emptyMap()
        imageMetadataLoaded = false
        _state.value = DriveConnectionState(phase = DriveConnectionPhase.FAILED)
        val current = _planState.value
        _planState.value = if (current.plans.isEmpty()) {
            DrivePlanState(
                phase = DrivePlanPhase.FAILED,
                externalCards = current.externalCards,
                foods = current.foods,
                errorMessage = "Google Driveの認証に失敗しました。",
            )
        } else {
            current.copy(
                phase = DrivePlanPhase.READY,
                errorMessage = "Driveを更新できないため、保存済みデータを表示しています。",
            )
        }
    }

    fun markOffline() {
        accessToken = null
        imageMetadata = emptyMap()
        imageMetadataLoaded = false
        _state.value = DriveConnectionState(phase = DriveConnectionPhase.FAILED)
        val current = _planState.value
        _planState.value = if (current.plans.isEmpty()) {
            DrivePlanState(
                phase = DrivePlanPhase.FAILED,
                externalCards = current.externalCards,
                foods = current.foods,
                errorMessage = "オフラインのため、保存済みデータのみ利用できます。",
            )
        } else {
            current.copy(
                phase = DrivePlanPhase.READY,
                errorMessage = "オフラインのため、保存済みデータを表示しています。",
            )
        }
    }

    suspend fun restoreCachedPlans() {
        val cached = runCatching { cacheRepository.load() }.getOrNull() ?: return
        val catalog = cached.catalog.preserveImagePathsFrom(_planState.value)
        val selectedPlanId = cached.selectedPlanId
            ?.takeIf { selectedId -> catalog.plans.any { it.id == selectedId } }
            ?: catalog.preferredPlanId
                ?.takeIf { preferredId -> catalog.plans.any { it.id == preferredId } }
            ?: catalog.plans.firstOrNull()?.id
        _planState.value = DrivePlanState(
            phase = DrivePlanPhase.READY,
            plans = catalog.plans,
            externalCards = catalog.externalCards,
            foods = catalog.foods,
            selectedPlanId = selectedPlanId,
        )
        selectedPlanId?.let { selectPlan(it) }
    }

    suspend fun fetchCalorieSummary(token: String): DriveCalorieSummary? {
        require(token.isNotBlank()) { "Googleアクセストークンが空です。" }
        accessToken = token
        return capture { client.readEstimatedCalorieSummary(token) }.getOrNull()
    }

    fun publishCalorieSummary(
        summary: DriveCalorieSummary?,
        updateVisibleValue: Boolean = true,
    ) {
        if (summary?.hasData != true) return
        calorieSummaryCache.save(summary)
        if (updateVisibleValue) {
            _calorieSummary.value = summary
        }
    }

    suspend fun connect(token: String) {
        require(token.isNotBlank()) { "Googleアクセストークンが空です。" }
        _state.value = DriveConnectionState(phase = DriveConnectionPhase.CONNECTING)
        val cachedState = _planState.value
        _planState.value = if (cachedState.plans.isEmpty()) {
            DrivePlanState(
                phase = DrivePlanPhase.LOADING,
                externalCards = cachedState.externalCards,
                foods = cachedState.foods,
            )
        } else {
            cachedState.copy(errorMessage = null)
        }
        accessToken = token
        imageMetadata = emptyMap()
        imageMetadataLoaded = false

        val loadResults = coroutineScope {
            val appData = async { capture { client.inspectAppData(token) } }
            val backup = async { capture { client.inspectBackupFolder(token) } }
            val plan = async { capture { planReader.load(token) } }
            DriveStartupLoadResults(
                appData = appData.await(),
                backup = backup.await(),
                plan = plan.await(),
            )
        }
        val appDataResult = loadResults.appData
        val backupResult = loadResults.backup
        val planResult = loadResults.plan
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

        val catalog = planResult.getOrNull()
        if (catalog == null) {
            val current = _planState.value
            _planState.value = if (current.plans.isEmpty()) {
                DrivePlanState(
                    phase = DrivePlanPhase.FAILED,
                    externalCards = current.externalCards,
                    foods = current.foods,
                    errorMessage = "Driveの食事プランを読み込めません。",
                )
            } else {
                current.copy(
                    phase = DrivePlanPhase.READY,
                    errorMessage = "Driveを更新できないため、保存済みデータを表示しています。",
                )
            }
            return
        }

        val previousSelectedPlanId = _planState.value.selectedPlanId
        val selectedPlanId = previousSelectedPlanId
            ?.takeIf { previousId -> catalog.plans.any { plan -> plan.id == previousId } }
            ?: catalog.preferredPlanId
            ?: catalog.plans.firstOrNull { it.isFavorite }?.id
            ?: catalog.plans.firstOrNull()?.id
        val displayCatalog = catalog.preserveImagePathsFrom(_planState.value)
        val cacheSaveError = runCatching {
            cacheRepository.save(catalog, selectedPlanId)
        }.exceptionOrNull()
        val visibleSelectedPlanId = _planState.value.selectedPlanId
            ?.takeIf { previousId -> displayCatalog.plans.any { plan -> plan.id == previousId } }
        _planState.value = DrivePlanState(
            phase = DrivePlanPhase.READY,
            plans = displayCatalog.plans,
            externalCards = displayCatalog.externalCards,
            foods = displayCatalog.foods,
            selectedPlanId = visibleSelectedPlanId,
            errorMessage = cacheSaveError?.let { "最新のDriveデータを端末へ保存できません。" },
        )
        selectedPlanId?.let { selectPlan(it) }
        if (cacheSaveError != null) {
            _planState.update { it.copy(errorMessage = "最新のDriveデータを端末へ保存できません。") }
        }
    }

    suspend fun selectPlan(planId: String) {
        val current = _planState.value
        require(current.plans.any { it.id == planId }) { "指定されたDriveプランが見つかりません。" }
        val selectedPlan = current.plans.first { it.id == planId }
        runCatching { shortcutSynchronizer.sync(selectedPlan) }
        runCatching { cacheRepository.saveSelectedPlanId(planId) }
        _planState.value = current.copy(
            selectedPlanId = planId,
            imageLoading = true,
            errorMessage = null,
        )
        val imageResult = capture {
            loadImages(
                token = accessToken,
                plan = selectedPlan,
                externalCards = current.externalCards,
                foods = current.foods,
            )
        }
        val loaded = imageResult.getOrNull()
        val latest = _planState.value
        val updatedPlans = if (loaded == null) {
            latest.plans
        } else {
            latest.plans.map { plan ->
                if (plan.id == planId) applyImagePaths(plan, loaded.paths) else plan
            }
        }
        val updatedExternalCards = if (loaded == null) {
            latest.externalCards
        } else {
            latest.externalCards.map { card ->
                card.copy(
                    imagePath = card.imageContentHash?.let { hash ->
                        loaded.paths[hash.lowercase()] ?: card.imagePath
                    },
                    items = card.items.map { item ->
                        item.copy(
                            imagePath = item.imageContentHash?.let { hash ->
                                loaded.paths[hash.lowercase()] ?: item.imagePath
                            },
                        )
                    },
                )
            }
        }
        val updatedFoods = if (loaded == null) {
            latest.foods
        } else {
            latest.foods.map { food ->
                food.copy(
                    imagePath = food.imageContentHash?.let { hash ->
                        loaded.paths[hash.lowercase()] ?: food.imagePath
                    },
                )
            }
        }
        _planState.value = latest.copy(
            plans = updatedPlans,
            externalCards = updatedExternalCards,
            foods = updatedFoods,
            imageLoading = false,
            errorMessage = when {
                imageResult.isFailure -> "Driveの画像情報を読み込めません。"
                (loaded?.failedCount ?: 0) > 0 -> "一部の食事画像を読み込めません。"
                else -> null
            },
        )
    }

    suspend fun downloadFile(fileId: String): ByteArray {
        val token = accessToken ?: error("Google Driveへ接続していません。")
        return client.downloadFile(token, fileId)
    }

    private suspend fun loadImages(
        token: String?,
        plan: DrivePlan,
        externalCards: List<DriveExternalCard>,
        foods: List<DriveFood>,
    ): ImageLoadResult {
        val hashes = plan.meals
            .flatMap { meal ->
                buildList {
                    meal.imageContentHash?.let(::add)
                    meal.items.mapNotNull { it.imageContentHash }.forEach(::add)
                }
            }
            .plus(
                externalCards.flatMap { card ->
                    buildList {
                        card.imageContentHash?.let(::add)
                        card.items.mapNotNull { it.imageContentHash }.forEach(::add)
                    }
                },
            )
            .plus(foods.mapNotNull { it.imageContentHash })
            .distinctBy { it.lowercase() }
        if (hashes.isEmpty()) return ImageLoadResult(emptyMap(), failedCount = 0)

        if (token != null && !imageMetadataLoaded) {
            imageMetadata = client.listImages(token)
                .associateBy { metadata -> metadata.contentHash.lowercase() }
            imageMetadataLoaded = true
        }

        val paths = linkedMapOf<String, String>()
        var failedCount = 0
        hashes.forEach { hash ->
            val cachedPath = imageCache.existingPath(hash)
            if (cachedPath != null) {
                paths[hash.lowercase()] = cachedPath
                return@forEach
            }
            if (token == null) return@forEach
            val metadata = imageMetadata[hash.lowercase()] ?: return@forEach
            runCatching {
                imageCache.store(hash, client.downloadFile(token, metadata.id))
            }.onSuccess { path ->
                paths[hash.lowercase()] = path
            }.onFailure {
                failedCount++
            }
        }
        return ImageLoadResult(paths, failedCount)
    }

    private fun applyImagePaths(
        plan: DrivePlan,
        paths: Map<String, String>,
    ): DrivePlan = plan.copy(
        meals = plan.meals.map { meal ->
            meal.copy(
                imagePath = meal.imageContentHash?.let { hash ->
                    paths[hash.lowercase()] ?: meal.imagePath
                },
                items = meal.items.map { item ->
                    item.copy(
                        imagePath = item.imageContentHash?.let { hash ->
                            paths[hash.lowercase()] ?: item.imagePath
                        },
                    )
                },
            )
        },
    )

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

    private data class ImageLoadResult(
        val paths: Map<String, String>,
        val failedCount: Int,
    )

    private data class DriveStartupLoadResults(
        val appData: Result<DriveAppDataSummary>,
        val backup: Result<DriveBackupSummary>,
        val plan: Result<DrivePlanCatalog>,
    )
}
