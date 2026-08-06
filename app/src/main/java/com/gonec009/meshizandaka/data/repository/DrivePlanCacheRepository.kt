package com.gonec009.meshizandaka.data.repository

import com.gonec009.meshizandaka.data.drive.DrivePlanCacheCodec
import com.gonec009.meshizandaka.data.drive.DrivePlanCatalog
import com.gonec009.meshizandaka.data.drive.GoogleDriveClient
import com.gonec009.meshizandaka.data.local.dao.DrivePlanCacheDao
import com.gonec009.meshizandaka.data.local.entity.DrivePlanCacheEntity

data class CachedDrivePlans(
    val catalog: DrivePlanCatalog,
    val selectedPlanId: String?,
)

class DrivePlanCacheRepository(
    private val dao: DrivePlanCacheDao,
) {
    suspend fun load(): CachedDrivePlans? {
        val entity = dao.get(GoogleDriveClient.DATASET_ID) ?: return null
        val catalog = runCatching { DrivePlanCacheCodec.decode(entity.catalogJson) }.getOrNull()
            ?: return null
        return CachedDrivePlans(
            catalog = catalog,
            selectedPlanId = entity.selectedPlanId,
        )
    }

    suspend fun save(catalog: DrivePlanCatalog, selectedPlanId: String?) {
        dao.upsert(
            DrivePlanCacheEntity(
                datasetId = GoogleDriveClient.DATASET_ID,
                catalogJson = DrivePlanCacheCodec.encode(catalog),
                selectedPlanId = selectedPlanId,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveSelectedPlanId(selectedPlanId: String) {
        dao.updateSelection(
            datasetId = GoogleDriveClient.DATASET_ID,
            selectedPlanId = selectedPlanId,
        )
    }
}
