package com.gonec009.meshizandaka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gonec009.meshizandaka.data.local.entity.DrivePlanCacheEntity

@Dao
interface DrivePlanCacheDao {
    @Query("SELECT * FROM drive_plan_cache WHERE datasetId = :datasetId LIMIT 1")
    suspend fun get(datasetId: String): DrivePlanCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DrivePlanCacheEntity)

    @Query(
        "UPDATE drive_plan_cache " +
            "SET selectedPlanId = :selectedPlanId " +
            "WHERE datasetId = :datasetId",
    )
    suspend fun updateSelection(
        datasetId: String,
        selectedPlanId: String,
    )
}
